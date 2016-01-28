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
import java.util.List;

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
		if (os instanceof PrintStream)
			stdout = (PrintStream) os;
		else
			stdout = new PrintStream(os);
	}

	public void setStderr(OutputStream os) {
		if (os instanceof PrintStream)
			stderr = (PrintStream) os;
		else
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
			expArgs = shell.expand(args);
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

			@Override
			public void setStdin(InputStream is) {
				super.setStdin(is);
				p1.stdin = is;
				p1.stdinReader = this.stdinReader;
				p2.stdin = is;
				p2.stdinReader = this.stdinReader;
			}

			@Override
			public void setStdout(OutputStream os) {
				super.setStdout(os);
				p1.stdout = this.stdout;
				p2.stdout = this.stdout;
			}

			@Override
			public void setStderr(OutputStream os) {
				super.setStderr(os);
				p1.stderr = this.stderr;
				p2.stderr = this.stderr;
			}
		};

		newP.setStdin(p1.stdin);
		newP.setStdout(p2.stdout);
		newP.setStderr(p2.stderr);
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

			@Override
			public void setStdin(InputStream is) {
				super.setStdin(is);
				p1.stdin = is;
				p1.stdinReader = this.stdinReader;
				p2.stdin = is;
				p2.stdinReader = this.stdinReader;
			}

			@Override
			public void setStdout(OutputStream os) {
				super.setStdout(os);
				p1.stdout = this.stdout;
				p2.stdout = this.stdout;
			}

			@Override
			public void setStderr(OutputStream os) {
				super.setStderr(os);
				p1.stderr = this.stderr;
				p2.stderr = this.stderr;
			}
		};

		newP.setStdin(p1.stdin);
		newP.setStdout(p2.stdout);
		newP.setStderr(p2.stderr);
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
	 * Return the absolute path corresponding to the given path. We cannot trust
	 * of File.getAbsolutePath(). Wildchards supported.
	 */
	protected String getAbsolutePath(String path) {

		return shell.getAbsolutePath(path);
	}

}
