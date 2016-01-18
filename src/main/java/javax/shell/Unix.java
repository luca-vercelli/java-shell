package javax.shell;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * It is useful to have all *NIX commands inside this class (it's easier to
 * import). Import using:
 * 
 * import static javax.shell.Unix.*;
 * 
 * @author luca vercelli 2016
 * 
 */
public class Unix {

	/**
	 * *NIX program <code>echo</code> (i.e. print to stdout).
	 */
	public static Program echo(String... args) {
		return new Program(args) {
			@Override
			public void runme() throws Exception {
				for (String s : args)
					System.out.print(s + " ");
				System.out.println();
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
	public static Program ls(String... args) {
		return new Program(args) {
			@Override
			public void runme() throws FileNotFoundException {
				if (args.isEmpty())
					args.add(".");

				for (String arg : args) {
					File[] files = ls1(arg);
					for (File f : files)
						System.out.println(f.getName());
				}
			}
		};
	}

	/**
	 * *NIX program <code>cp</code> (i.e. copy files).
	 */
	public static Program cp(String options, String... srcAndDest) {
		throw new IllegalStateException("Not implemented");
	}

	/**
	 * *NIX program <code>rm</code> (i.e. remove files).
	 */
	public static Program rm(String options, String... src) {
		throw new IllegalStateException("Not implemented");
	}

	/**
	 * *NIX program <code>mv</code> (i.e. move files).
	 */
	public static Program mv(String options, String... srcAndDest) {
		throw new IllegalStateException("Not implemented");
	}

	/**
	 * *NIX program <code>cat</code> (i.e. print content of files).
	 */
	public static Program cat(String options, String... src) {
		return new Program(src) {
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
	public static Program grep(String options, final String text, String... src) {
		return new Program(src) {
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
	 * *NIX program <code>true</code> (i.e. always succeed).
	 */
	public static Program true_() {
		return new Program() {
			@Override
			public void runme() {

			}
		};
	}

	/**
	 * *NIX program <code>false</code> (i.e. fails always).
	 */
	public static Program false_() {
		return new Program() {
			@Override
			public void runme() {
				throw new IllegalStateException("false");
			}
		};
	}

	/**
	 * *NIX program <code>find</code> (i.e. search for files).
	 */
	public static Program find(String options, String folder) {
		throw new IllegalStateException("Not implemented");
	}

	/**
	 * *NIX program <code>pwd</code> (i.e. print working directory).
	 */
	public static Program pwd() {
		throw new IllegalStateException("Not implemented");
	}

	/**
	 * *NIX program <code>cd</code> (i.e. change directory).
	 */
	public static Program cd(String folder) {
		throw new IllegalStateException("Not implemented");
	}
}
