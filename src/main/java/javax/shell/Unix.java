package javax.shell;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.EmptyStackException;
import java.util.List;
import java.util.Stack;

/**
 * It is useful to have all *NIX commands inside this class (it's easier to
 * import). Import using:
 * 
 * import static javax.shell.Unix.*;
 * 
 * All these routines return a {@see Process}. They should follow common
 * command-line standards:
 * <ul>
 * <li>Support for stdin and stdout
 * <li>Extensive use of "..." arguments
 * <li>Respect the "current folder".
 * </ul>
 * 
 * @author luca vercelli 2016
 * 
 */
public class Unix {

	/**
	 * *NIX shell command <code>pwd</code> (i.e. print working directory).
	 */
	public static Process pwd() {
		return new Process() {
			@Override
			public void runme() throws Exception {
				stdout.println(Process.getCurrentFolder());
			}
		};
	}

	/**
	 * *NIX shell command <code>cd</code> (i.e. change directory).
	 */
	public static Process cd(final String folder) {
		return new Process() {
			@Override
			public void runme() throws Exception {
				Process.setCurrentFolder(getAbsolutePath(folder));
			}
		};
	}

	private static Stack<String> historyFolders = new Stack<String>();

	/**
	 * *NIX shell command <code>pushd</code> (i.e. change directory saving
	 * current one).
	 */
	public static Process pushd(final String folder) {
		return new Process() {
			@Override
			public void runme() throws Exception {
				historyFolders.push(Process.getCurrentFolder());
				Process.setCurrentFolder(getAbsolutePath(folder));
			}
		};
	}

	/**
	 * *NIX shell command <code>pushd</code> (i.e. change directory saving
	 * current one). An {@see EmptyStackException} will be thrown if no
	 * <code>pushd</code> were called before.
	 */
	public static Process popd() {
		return new Process() {
			@Override
			public void runme() throws EmptyStackException {
				Process.setCurrentFolder(historyFolders.pop());
			}
		};
	}

	/**
	 * *NIX shell command <code>echo</code> (i.e. print to stdout).
	 */
	public static Process echo(String... args) {
		return new Process(args) {
			@Override
			public void runme() throws Exception {
				for (String s : args)
					stdout.print(s + " ");
				stdout.println();
			}
		};
	}

	private static File[] ls1(String arg) throws FileNotFoundException {
		File f = new File(arg);
		if (!f.exists())
			throw new FileNotFoundException();
		if (f.isDirectory())
			return f.listFiles();
		else
			return new File[] { f };
	}

	/**
	 * *NIX program <code>ls</code> (i.e. list directory).
	 */
	public static Process ls(String... args) {
		return new Process(args) {
			@Override
			public void runme() throws FileNotFoundException {
				if (args.isEmpty())
					args.add(".");

				for (String arg : args) {
					File[] files = ls1(arg);
					for (File f : files)
						stdout.println(f.getName());
				}
			}
		};
	}

	/**
	 * *NIX program <code>cp</code> (i.e. copy files).
	 */
	public static Process cp(String... srcAndDest) {
		throw new IllegalStateException("Not implemented");
	}

	/**
	 * *NIX program <code>rm</code> (i.e. remove files).
	 */
	public static Process rm(String... src) {
		throw new IllegalStateException("Not implemented");
	}

	/**
	 * *NIX program <code>mv</code> (i.e. move files).
	 */
	public static Process mv(String options, String... srcAndDest) {
		throw new IllegalStateException("Not implemented");
	}

	/**
	 * *NIX program <code>mkdir</code> (i.e. make directory).
	 */
	public static Process mkdir(String... folders) {
		throw new IllegalStateException("Not implemented");
	}

	/**
	 * *NIX program <code>mkdir -p</code> (i.e. make directory including all
	 * upper levels).
	 */
	public static Process mkdir_p(String... folders) {
		throw new IllegalStateException("Not implemented");
	}

	/**
	 * *NIX program <code>rmdir</code> (i.e. remove empty directories).
	 */
	public static Process rmdir(String... folders) {
		throw new IllegalStateException("Not implemented");
	}

	/**
	 * *NIX program <code>ln -s</code> (i.e. create symbolic link).
	 */
	public static Process ln_s(String src, String dest) {
		throw new IllegalStateException("Not implemented");
	}

	/**
	 * *NIX program <code>cat</code> (i.e. print content of files).
	 */
	public static Process cat(String... src) {
		return new Process(src) {
			@Override
			public void runme() throws IOException {

				List<InputStream> sources = this.getInputStreams(args);
				for (InputStream is : sources) {
					byte[] buffer = new byte[1024];
					int len;
					while ((len = is.read(buffer)) != -1) {
						stdout.write(buffer, 0, len);
					}
				}
			}
		};
	}

	/**
	 * *NIX program <code>grep</code> (i.e. search for text lines matching some
	 * pattern).
	 */
	public static Process grep(final String text, String... src) {
		return new Process(src) {
			@Override
			public void runme() throws IOException {

				List<BufferedReader> sources = this.getReaders(args);
				for (BufferedReader is : sources) {
					while (is.ready()) {
						String line = is.readLine();
						if (line != null && line.contains(text))
							stdout.println(line);
					}
				}
			}
		};
	}

	/**
	 * *NIX program <code>grep -v</code> (i.e. search for text lines <i>not</i>
	 * matching some pattern).
	 */
	public static Process grep_v(final String text, String... src) {
		return new Process(src) {
			@Override
			public void runme() throws IOException {

				List<BufferedReader> sources = this.getReaders(args);
				for (BufferedReader is : sources) {
					while (is.ready()) {
						String line = is.readLine();
						if (line != null && !line.contains(text))
							stdout.println(line);
					}
				}
			}
		};
	}

	/**
	 * *NIX program <code>true</code> (i.e. always succeed).
	 */
	public static Process true_() {
		return new Process() {
			@Override
			public void runme() {

			}
		};
	}

	/**
	 * *NIX program <code>false</code> (i.e. fail always).
	 */
	public static Process false_() {
		return new Process() {
			@Override
			public void runme() {
				throw new IllegalStateException("false");
			}
		};
	}

	/**
	 * *NIX program <code>find</code> (i.e. search for files).
	 */
	public static Process find(String options, String folder) {
		throw new IllegalStateException("Not implemented");
	}

}
