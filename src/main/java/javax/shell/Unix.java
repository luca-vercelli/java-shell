package javax.shell;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
				for (String s : expArgs)
					stdout.print(s + " ");
				stdout.println();
			}
		};
	}

	/**
	 * *NIX program <code>ls</code> (i.e. list directory). Do not print hidden
	 * files.
	 */
	public static Process ls(String... args) {
		return new Process(args) {
			@Override
			public void runme() throws FileNotFoundException {
				if (expArgs.isEmpty())
					expArgs.add(".");

				for (String arg : expArgs) {
					File[] files = ls1(getAbsolutePath(arg));
					for (File f : files)
						stdout.println(f.getName());
				}
			}

			private File[] ls1(String arg) throws FileNotFoundException {
				File f = new File(arg);
				if (!f.exists())
					throw new FileNotFoundException();
				if (f.isDirectory())
					return f.listFiles();
				else
					return new File[] { f };
			}

		};
	}

	/**
	 * *NIX program <code>cp</code> (i.e. copy files).
	 * 
	 * @param sourcesAndDest
	 *            At least one source file or folder, and exactly one
	 *            destination folder.
	 */
	public static Process cp(String... sourcesAndDest) {
		return new Process(sourcesAndDest) {
			@Override
			public void runme() throws IOException {
				if (expArgs.size() < 2)
					throw new IllegalArgumentException("cp requires at least one source and the destination");

				File destDir = new File(getAbsolutePath(expArgs.get(expArgs.size() - 1)));

				if (expArgs.size() == 2 && !destDir.exists() && destDir.getParentFile().isDirectory()) {
					// different algorithm: in this case, we just copy the file
					// with a new name
					File src = new File(getAbsolutePath(expArgs.get(0)));
					copy(src, destDir);

				} else {

					if (!destDir.isDirectory())
						throw new IllegalArgumentException(destDir.getPath() + " does not exist or is not a folder");

					for (int i = 0; i < expArgs.size() - 1; ++i) {
						File src = new File(getAbsolutePath(expArgs.get(i)));
						if (src.isDirectory())
							throw new IllegalArgumentException(src.getPath() + " is a directory");
						File dest = new File(destDir, src.getName());
						if (dest.exists())
							throw new IllegalArgumentException(dest.getPath() + " already exists");
						copy(src, dest);
					}
				}
			}

			private void copy(File fileSrc, File fileDest) throws IOException {
				Path src = Paths.get(fileSrc.getPath());
				OutputStream destStream = new FileOutputStream(fileDest);
				Files.copy(src, destStream);
				destStream.close();
			}
		};
	}

	/**
	 * *NIX program <code>cp -r</code> (i.e. copy files and folders
	 * recursively).
	 * 
	 * @param sourcesAndDest
	 *            At least one source file or folder, and exactly one
	 *            destination folder.
	 */
	public static Process cp_r(String... sourcesAndDest) {
		return new Process(sourcesAndDest) {
			@Override
			public void runme() throws IOException {
				if (expArgs.size() < 2)
					throw new IllegalArgumentException("cp requires at least one source and the destination");

				File destDir = new File(getAbsolutePath(expArgs.get(expArgs.size() - 1)));

				if (expArgs.size() == 2 && !destDir.exists() && destDir.getParentFile().isDirectory()) {
					// different algorithm: in this case, we just copy the file
					// with a new name
					File src = new File(getAbsolutePath(expArgs.get(0)));
					copyR(src, destDir);

				} else {

					if (!destDir.isDirectory())
						throw new IllegalArgumentException(destDir.getPath() + " does not exist or is not a folder");

					for (int i = 0; i < expArgs.size() - 1; ++i) {
						File src = new File(getAbsolutePath(expArgs.get(i)));
						File dest = new File(destDir, src.getName());
						if (dest.exists())
							throw new IllegalArgumentException(dest.getPath() + " already exists");
						copy(src, dest);
					}
				}
			}

			private void copy(File fileSrc, File fileDest) throws IOException {
				Path src = Paths.get(fileSrc.getPath());
				OutputStream destStream = new FileOutputStream(fileDest);
				Files.copy(src, destStream);
				destStream.close();
			}

			private void copyR(File src, File dest) throws IOException {
				if (src.isDirectory()) {
					dest.mkdir();
					File[] content = src.listFiles();
					for (File f : content) {
						copyR(f, new File(dest, f.getName()));
					}
				} else {
					copy(src, dest);
				}
			}
		};
	}

	/**
	 * *NIX program <code>mv</code> (i.e. move files).
	 * 
	 * @param sourcesAndDest
	 *            At least one source file or folder, and exactly one
	 *            destination folder.
	 */
	public static Process mv(String... sourcesAndDest) {
		return new Process(sourcesAndDest) {
			@Override
			public void runme() {
				if (expArgs.size() < 2)
					throw new IllegalArgumentException("mv requires at least one source and the destination");

				File destDir = new File(getAbsolutePath(expArgs.get(expArgs.size() - 1)));

				if (expArgs.size() == 2 && !destDir.exists() && destDir.getParentFile().isDirectory()) {
					// different algorithm: in this case, we just rename the
					// folder
					File src = new File(getAbsolutePath(expArgs.get(0)));
					src.renameTo(destDir);

				} else {

					if (!destDir.isDirectory())
						throw new IllegalArgumentException(destDir.getPath() + " does not exist or is not a folder");

					for (int i = 0; i < expArgs.size() - 1; ++i) {
						File src = new File(getAbsolutePath(expArgs.get(i)));

						File dest = new File(destDir, src.getName());
						if (dest.exists())
							throw new IllegalArgumentException(dest.getPath() + " already exists");

						src.renameTo(dest);
					}
				}
			}
		};
	}

	/**
	 * *NIX program <code>rm -f</code> (i.e. remove files). No error is given if
	 * file does not exists. An error is given if file is a directory.
	 */
	public static Process rm(String... src) {
		return new Process(src) {
			@Override
			public void runme() {
				for (String s : expArgs) {
					File f = new File(getAbsolutePath(s));
					if (!f.exists())
						continue;
					if (f.isDirectory())
						throw new IllegalArgumentException(f.getPath() + " is a directory");
					f.delete();
				}
			}
		};
	}

	/**
	 * *NIX program <code>rm -rf</code> (i.e. remove files and folders
	 * recursively). No error is given ithrow new
	 * IllegalArgumentException(f.getPath() + " is a directory");f file does not
	 * exists.
	 */
	public static Process rm_r(String... src) {
		return new Process(src) {
			@Override
			public void runme() {
				for (String s : expArgs) {
					File f = new File(getAbsolutePath(s));
					rmRec(f);
				}
			}

			private void rmRec(File f) {
				if (!f.exists())
					return;
				if (f.isDirectory()) {
					File[] content = f.listFiles();
					for (File g : content)
						rmRec(g);
				}
				f.delete();
			}
		};
	}

	/**
	 * *NIX program <code>mkdir</code> (i.e. make directory).
	 */
	public static Process mkdir(String... folders) {
		return new Process(folders) {
			@Override
			public void runme() {
				for (String s : expArgs)
					new File(getAbsolutePath(s)).mkdir();
			}
		};
	}

	/**
	 * *NIX program <code>mkdir -p</code> (i.e. make directory including all
	 * upper levels).
	 */
	public static Process mkdir_p(String... folders) {
		return new Process(folders) {
			@Override
			public void runme() {
				for (String s : expArgs)
					new File(getAbsolutePath(s)).mkdirs();
			}
		};
	}

	/**
	 * *NIX program <code>rmdir</code> (i.e. remove empty directories). No error
	 * is given if folder does not exists. An error is given if file is not a
	 * directory, or the directory is not empty.h hidden //
	 */
	public static Process rmdir(String... folders) {
		return new Process(folders) {
			@Override
			public void runme() {
				for (String s : expArgs) {
					File f = new File(getAbsolutePath(s));
					if (!f.exists())
						continue;
					if (!f.isDirectory())
						throw new IllegalArgumentException(f.getPath() + " is not a directory");
					if (f.list().length > 0) {
						// FIXME does not catch hidden files
						throw new IllegalArgumentException(f.getPath() + " is not empty");
					}
					f.delete();
				}
			}
		};
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

				List<InputStream> sources = this.getInputStreams(expArgs);
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

				List<BufferedReader> sources = this.getReaders(expArgs);
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

				List<BufferedReader> sources = this.getReaders(expArgs);
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

	/**
	 * *NIX program <code>wget</code> (i.e. download files from internet).
	 * Output to stdout.
	 */
	public static Process wget(String address) {
		throw new IllegalStateException("Not implemented");
	}

	/**
	 * *NIX program <code>wget</code> (i.e. download files from internet).
	 * 
	 * @throws IOException
	 */
	public static Process wget(String address, String localFile) throws IOException {
		return wget(address).redirect(localFile);
	}

}
