package javax.shell;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
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
 * TODO: we don't have a good options handling system yet.
 * 
 * @author luca vercelli 2016
 * 
 */
public abstract class Process extends Thread {

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
	 * All arguments passed to the program, already shell-expanded.
	 */
	protected List<String> args = null;

	/**
	 * Previous {@see Process} in pipeline.
	 */
	protected Process prec = null;

	/**
	 * Recall that in Java there is not good "current folder", so we store it
	 * here.
	 * 
	 * This attribute is static, that means that we can have only one shell at
	 * the time. Please notice that, in a "philosophical" perspective, this is
	 * not an attribute of the Process, it is an attribute of the shell itself,
	 * meaning that different concurrent shell may have different current
	 * folders.
	 */
	private static String currentFolder = System.getProperty("user.dir");

	// absolute path.

	public Process(List<String> args) {
		this.args = expand(args);
	}

	public Process(String[] args) {
		this.args = expand(Arrays.asList(args));
	}

	public Process() {
		this.args = new ArrayList<String>();
	}

	public void setStdin(InputStream is) {
		stdin = is;
		stdinReader = new BufferedReader(new InputStreamReader(stdin));
	}

	public void setStdout(OutputStream os) {
		stdout = new PrintStream(os);
	}

	public static String getCurrentFolder() {
		return currentFolder;
	}

	public static void setCurrentFolder(String currentFolder) {
		Process.currentFolder = currentFolder;
	}

	/**
	 * Starting a Program will start all the pipeline before it.
	 */
	@Override
	public void start() {
		if (prec != null)
			prec.start();
		super.start();
	}

	/**
	 * The program body shall be put inside the "runme" method.
	 */
	@Override
	public void run() {
		if (prec != null)
			prec.start();
		try {
			runme();
		} catch (Exception e) {
			System.err.println("Unhandled exception in Thread " + this.getName());
			e.printStackTrace(System.err);
			System.exit(-1); // stop all threads...
		}
	}

	/**
	 * Program body. Differently from <code>run()</code>, this routine allows
	 * throwing Exceptions.
	 */
	public abstract void runme() throws Exception;

	/**
	 * Create a pipeline. The two programs are run in separated threads. Both
	 * Programs did not run yet. Intendend use:
	 * 
	 * p1.pipe(p2).pipe(p3).start()
	 * 
	 * @return the second Program p2
	 */
	public Process pipe(Process p2) throws IOException {
		PipedInputStream pis = new PipedInputStream();
		p2.setStdin(pis);
		this.setStdout(new PipedOutputStream(pis));
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
	 * @return the second Program p2
	 */
	public Process and(final Process p2) throws IOException {

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
	 * @return the second Program p
	 */
	public Process or(final Process p2) throws IOException {

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
		setStdout(new FileOutputStream(new File(file)));
		return this;
	}

	/**
	 * Redirect output to file, in append mode. In this implementation, two
	 * redirections are allowed, but useless.
	 * 
	 * @return this Program
	 */
	public Process append(String file) throws IOException {
		setStdout(new FileOutputStream(new File(file), true));
		return this;
	}

	/**
	 * Redirect input from an existing file. In this implementation, two
	 * redirections are allowed, but useless.
	 * 
	 * @return this Program
	 */
	public Process redirectFrom(String file) throws IOException {
		setStdin(new FileInputStream(file));
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

	private static void expandRecursive(File root, Stack<String> pieces, Set<String> ret) {

		if (!root.exists())
			return;

		System.out.println("DEBUG root=" + root.getAbsolutePath());

		if (pieces.isEmpty() || !root.isDirectory()) {
			ret.add(root.getPath());
			return;
		}

		String nextPiece = pieces.pop();
		nextPiece = nextPiece.replaceAll("\\*", "\\*").replaceAll("\\?", "\\?");
		final Pattern p = Pattern.compile(nextPiece);
		String[] files = root.list();/*
										 * new FilenameFilter() {
										 * 
										 * @Override public boolean accept(File
										 * dir, String filename) { return
										 * p.matcher(filename).matches(); } });
										 */
		for (String f : files) {
			expandRecursive(new File(root, f), pieces, ret);
		}
		pieces.push(nextPiece);
	}

	/**
	 * Shell-expansion of arguments
	 */
	public static List<String> expand(List<String> paths) {
		if (paths == null)
			throw new IllegalArgumentException("null paths given");

		Set<String> ret = new HashSet<String>();
		for (String path : paths) {

			if (path == null)
				continue; // should not happen ... but...

			if (!path.contains("*") && !path.contains("?"))
				ret.add(path); // e.g. a fixed filename, empty string, or
								// options

			// here, we *must* perform expansion
			if (File.separator.equals("/")) {
				// *NIX
				if (!path.startsWith("/") && !path.startsWith("./")) {
					path = "./" + path;
				}
			} else if (File.separator.equals("\\")) {
				// Windows
				if (path.charAt(0) != ':' && !path.startsWith(".\\")) {
					path = ".\\" + path;
				}
			} else {
				System.err.println("Unsupported operating system! Please report this.");
				return paths;
			}

			String[] pieces = path.split(File.separator);

			System.out.println("DEBUG: " + Arrays.asList(pieces));

			File root = new File(pieces[0].equals("") ? "/" : pieces[0]);

			Stack<String> piecesStack = new Stack<String>();
			for (int i = pieces.length - 1; i > 0; --i)
				piecesStack.push(pieces[i]);

			expandRecursive(root, piecesStack, ret);
		}
		return new ArrayList<String>(ret);
	}
}
