package javax.shell;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Pattern;

/**
 * Main class for *NIX based programs.
 * 
 * Programs have two main features: arguments expansion and pipelines. Es.
 * 
 * p1.pipe(p2).pipe(p3).start()
 * 
 * Differently from traditional shell programs, but more closed to Java spirit,
 * programs launch Exceptions instead of returning an error code.
 * 
 * If you want run a single Process, without pipelining, you can just call
 * <code>.run()</code> instead of <code>.start()</code>. If you have built a
 * pipeline, and you want wait for it to terminate before continuing, then you
 * can call <code>.sh()</code> instead of <code>.start()</code>. Thinking about
 * shells, <code>.sh()</code> is the "usual" operation that the shell performs
 * at end of line; <code>.start()</code> is the operation performed if an "&" is
 * written at the end of the command.
 * 
 * TODO: we don't have a good options handling system yet.
 * 
 * @author luca vercelli 2016
 * 
 */
public abstract class Process extends Thread {

	public final static int BUFFER_SIZE = 2048;

	/**
	 * This should be a possible value for stdout, doing nothing.
	 */
	public static PrintStream DEV_NULL = null;

	static {
		try {
			DEV_NULL = new PrintStream(new File("/dev/null"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		// FIXME unix only
	}

	/**
	 * Standard input
	 */
	protected InputStream stdin = System.in;

	/**
	 * Standard input, supporting readLine(). This must <b>always</b> point to
	 * stream stdin.
	 */
	protected BufferedReader stdinReader = new BufferedReader(new InputStreamReader(System.in));

	/**
	 * Standard output
	 */
	protected PrintStream stdout = System.out;

	/**
	 * Standard error
	 */
	protected PrintStream stderr = System.err;

	/**
	 * All arguments passed to the program, in their original form.
	 */
	protected List<String> args = null;
	/**
	 * All arguments passed to the program, already shell-expanded.
	 */
	protected List<String> expArgs = null;

	/**
	 * Previous {@see Process} in pipeline.
	 */
	protected Process prec = null;

	/**
	 * Shell in which the Program is executed
	 */
	protected Shell shell = Shell.getInstance();

	public Process(List<String> args) {
		this.args = args;
	}

	public Process(String[] args) {
		this(Arrays.asList(args));
	}

	public Process() {
		this(new ArrayList<String>());
	}

	public Process(String arg) {
		this(new String[] { arg });
	}

	public void setStdin(InputStream is) {
		stdin = is;
		stdinReader = new BufferedReader(new InputStreamReader(stdin));
	}

	public void setStdout(OutputStream os) {
		stdout = new PrintStream(os);
	}

	public void setStderr(OutputStream os) {
		stderr = new PrintStream(os);
	}

	public String getCurrentFolder() {
		return shell.getCurrentFolder();
	}

	public void setCurrentFolder(String currentFolder) {
		shell.setCurrentFolder(currentFolder);
	}

	public List<String> expArgs() {
		if (expArgs == null) {
			expArgs = expand(args);
		}
		return expArgs;
	}

	/**
	 * Starting a {@Process} will start all the pipeline before it.
	 */
	@Override
	public void start() {
		if (prec != null)
			prec.start();
		super.start();
	}

	/**
	 * Start the {@Process}, then wait for it to finish.
	 */
	public void sh() {
		start();
		try {
			join();
		} catch (InterruptedException e) {
			// somebody stopped the thread?!?
			throw new RuntimeException(e);
		}
	}

	/**
	 * The program body shall be put inside the "runme" method.
	 */
	@Override
	public void run() {
		try {
			runme();
		} catch (Exception e) {
			System.err.println("Unhandled exception in Thread " + this.getName());
			e.printStackTrace(System.err);
		}
		if ((stdout != null) && (stdout != System.out))
			try {
				stdout.flush();
				stdout.close();
			} catch (RuntimeException ne) {
			}
	}

	/**
	 * Program body. Differently from <code>run()</code>, this routine allows
	 * throwing Exceptions.
	 */
	public abstract void runme() throws Exception;

	/**
	 * Create a pipeline. The two programs are run in separated threads. Both
	 * Programs did not run yet. Intended use:
	 * 
	 * p1.pipe(p2).pipe(p3).sh()
	 * 
	 * @return the second Process p2
	 * @throws IOException
	 */
	public Process pipe(Process p2) {
		PipedInputStream pis = new PipedInputStream(BUFFER_SIZE);
		PipedOutputStream pos;
		try {
			pos = new PipedOutputStream(pis);
		} catch (IOException e) {
			throw new RuntimeException(e); // why should this happen?
		}

		p2.setStdin(pis);
		this.setStdout(pos);
		p2.prec = this;
		return p2;
	}

	/**
	 * Run the two programs one after the other, and the second program is run
	 * if and only if the first one succeeded. Similar to the "&&" *NIX-shell
	 * operator. The two programs are run in the same thread.
	 * 
	 * p1.and(p2).start()
	 * 
	 * @return the new Process
	 */
	public Process and(final Process p2) {

		final Process p1 = this;

		Process newP = new Process() {

			@Override
			public void runme() throws Exception {
				p1.runme();
				p2.runme();
			}
		};

		newP.stdin = p1.stdin;
		newP.stdout = p2.stdout;
		newP.prec = p1.prec;
		return newP;
	}

	/**
	 * Run the two programs one after the other, and the second program is run
	 * if and only if the first one failed. Similar to the "||" *NIX-shell
	 * operator. The two programs are run in the same thread.
	 * 
	 * p1.or(p2).start()
	 * 
	 * @return the new Process
	 */
	public Process or(final Process p2) {

		final Process p1 = this;

		Process newP = new Process() {

			@Override
			public void runme() throws Exception {
				try {
					p1.runme();
				} catch (Exception e) {
					p2.runme();
				}
			}
		};

		newP.stdin = p1.stdin;
		newP.stdout = p2.stdout;
		newP.prec = p1.prec;
		return newP;
	}

	/**
	 * Redirect output to file. In this implementation, two redirections are
	 * allowed, but useless.
	 * 
	 * @return this Program
	 */
	public Process redirect(String file) throws IOException {
		setStdout(new FileOutputStream(getAbsolutePath(file)));
		return this;
	}

	/**
	 * Redirect output to file, in append mode. In this implementation, two
	 * redirections are allowed, but useless.
	 * 
	 * @return this Program
	 */
	public Process append(String file) throws IOException {
		setStdout(new FileOutputStream(getAbsolutePath(file), true));
		return this;
	}

	/**
	 * Redirect input from an existing file. In this implementation, two
	 * redirections are allowed, but useless.
	 * 
	 * @return this Program
	 */
	public Process redirectFrom(String file) throws IOException {
		setStdin(new FileInputStream(getAbsolutePath(file)));
		return this;
	}

	/**
	 * Redirect output to file. In this implementation, two redirections are
	 * allowed, but useless.
	 * 
	 * @return this Program
	 */
	public Process redirectErr(String file) throws IOException {
		setStderr(new FileOutputStream(getAbsolutePath(file)));
		return this;
	}

	/**
	 * Remove first argument from array. Modify List in-place.
	 */
	public static List<String> shift(List<String> args) {
		args.remove(0);
		return args;
	}

	/**
	 * Create a list of FileInputStream, if any, or stdin.
	 */
	public List<InputStream> getInputStreams(List<String> files) throws FileNotFoundException {
		List<InputStream> ret = new ArrayList<InputStream>();
		if (files.isEmpty())
			ret.add(stdin);
		else
			for (String file : files) {
				ret.add(new FileInputStream(file));
			}
		return ret;
	}

	/**
	 * Create a list of BufferedReader, if any, or stdin.
	 */
	public List<BufferedReader> getReaders(List<String> files) throws FileNotFoundException {
		List<BufferedReader> ret = new ArrayList<BufferedReader>();
		if (files.isEmpty())
			ret.add(stdinReader);
		else
			for (String file : files) {
				ret.add(new BufferedReader(new FileReader(file)));
			}
		return ret;
	}

	/**
	 * Create a list of FileOutputStream, if any, or stdout.
	 */
	public List<PrintStream> getOutputStreams(List<String> files, boolean append) throws FileNotFoundException {
		List<PrintStream> ret = new ArrayList<PrintStream>();
		if (files.isEmpty())
			ret.add(stdout);
		else
			for (String file : files) {
				ret.add(new PrintStream(new FileOutputStream(file, append)));
			}
		return ret;
	}

	/**
	 * Return true if path is absolute (i.e. /some/p*ath/ or C:\so?me\path ).
	 */
	protected static boolean isAbsolute(String path) {

		if (path == null)
			throw new IllegalArgumentException("null path given");
		path = path.trim();
		if (path.equals(""))
			throw new IllegalArgumentException("empty path given");

		if (File.separator.equals("/")) {
			// *NIX
			return path.length() >= 1 && path.charAt(0) == '/';

		} else if (File.separator.equals("\\")) {
			// Windows
			return path.length() >= 2 && path.charAt(1) == ':';

		} else {
			throw new IllegalStateException("Unsupported operating system! Please report this.");
		}
	}

	/**
	 * Split the path in pieces according to File.separator. The first piece is
	 * the "root": it is the path's root ( "/" or "C:\" ) if the given path is
	 * absolute, or current folder if it is relative.
	 */
	protected String[] splitRoot(String path) {

		String[] pieces = path.split(File.separator);

		if (!isAbsolute(path)) {

			String[] result = new String[pieces.length + 1];
			result[0] = getCurrentFolder();
			for (int i = 0; i < pieces.length; ++i)
				result[i + 1] = pieces[i];
			return result;

		} else {
			if (File.separator.equals("/")) {
				// *NIX
				pieces[0] = "/"; // instead of ""
				return pieces;

			} else if (File.separator.equals("\\")) {
				// Windows
				return pieces;

			} else {
				throw new IllegalStateException("Unsupported operating system! Please report this.");
			}
		}
	}

	/**
	 * Return the absolute path corresponding to the given path. We cannot trust
	 * of File.getAbsolutePath(). Wildchards supported.
	 */
	protected String getAbsolutePath(String path) {

		if (isAbsolute(path))
			return path;

		// here, the path is relative, and it is not null nor empty.

		return getCurrentFolder() + File.separator + path.trim();
	}

	private void expandRecursive(File root, Stack<String> pieces, Set<String> ret) {

		// DEBUG CODE
		// System.out.println("DEBUG. entering expandRecursive(" + root + ", " +
		// pieces + ", " + ret);

		if (!root.exists())
			return;

		if (pieces.isEmpty() || !root.isDirectory()) {
			ret.add(root.getPath());
			return;
		}

		String nextPiece = pieces.pop();
		if (!nextPiece.contains("*") && !nextPiece.contains("?")) {
			// We use a different algorithm in order to see hidden files too
			File son = new File(root, nextPiece);
			if (son.exists()) {
				expandRecursive(son, pieces, ret);
			}
		} else {

			final Pattern p = Pattern.compile(nextPiece.replace("*", ".*").replace("?", ".{1}"));
			String[] files = root.list(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String filename) {
					return p.matcher(filename).matches();
				}
			});

			for (String f : files) {
				expandRecursive(new File(root, f), pieces, ret);
			}
		}

		pieces.push(nextPiece);

		// DEBUG CODE
		// System.out.println("DEBUG. exiting expandRecursive(" + root + ", " +
		// pieces + ", " + ret);
	}

	/**
	 * Shell-expansion of arguments
	 */
	public List<String> expand(List<String> paths) {
		if (paths == null)
			throw new IllegalArgumentException("null paths given");

		// DEBUG CODE
		// System.out.println("expand:" + paths);

		Set<String> ret = new HashSet<String>();
		for (String path : paths) {

			if (path == null)
				continue; // should not happen ... but...

			if (!path.contains("*") && !path.contains("?")) {
				// e.g. a fixed filename, empty string, or options
				ret.add(path);
				continue;
			}

			// here, we *must* perform expansion

			String[] pieces = splitRoot(path);

			// DEBUG CODE
			System.err.println(
					"SPLITROOT path=" + path + " pieces=" + Arrays.asList(pieces) + " pwd=" + getCurrentFolder());

			File root = new File(pieces[0]);

			Stack<String> piecesStack = new Stack<String>();
			for (int i = pieces.length - 1; i > 0; --i)
				piecesStack.push(pieces[i]);

			expandRecursive(root, piecesStack, ret);
		}
		return new ArrayList<String>(ret);
	}
}
