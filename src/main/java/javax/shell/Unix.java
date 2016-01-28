package javax.shell;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EmptyStackException;
import java.util.List;
import java.util.Stack;

/**
 * This class contains only static methods that return a {@see Process}. All
 * these methods are useful for <i>scripting</i>, i.e. writing simple programs
 * with a few lines of code. The names of these methods recall common *NIX
 * commands.
 * 
 * It is useful to have all these commands inside this class, because it's
 * easier to import. Import using:
 * 
 * <code>import static javax.shell.Unix.*;</code>
 * 
 * All these static methods return a {@see Process}. They should follow common
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
	 * Print working directory.
	 */
	public static Process pwd() {
		return new Process() {
			@Override
			public void runme() throws Exception {
				stdout.println(getCurrentFolder());
			}
		};
	}

	/**
	 * Change directory.
	 */
	public static Process cd(String folder) {
		return new Process(folder) {
			@Override
			public void runme() {
				if (expArgs().isEmpty())
					throw new IllegalArgumentException("Argument missing");
				setCurrentFolder(getAbsolutePath(expArgs().get(0)));
			}
		};
	}

	private static Stack<String> historyFolders = new Stack<String>();

	/**
	 * Change directory, pushing the current one in an internal stack.
	 */
	public static Process pushd(final String folder) {
		return new Process() {
			@Override
			public void runme() throws Exception {
				historyFolders.push(getCurrentFolder());
				setCurrentFolder(getAbsolutePath(folder));
			}
		};
	}

	/**
	 * Change directory, popping it from an internal stack (saved by
	 * <code>pushd</code>).
	 * 
	 * @throws EmptyStackException
	 *             if no <code>pushd</code> were called before.
	 */
	public static Process popd() {
		return new Process() {
			@Override
			public void runme() throws EmptyStackException {
				setCurrentFolder(historyFolders.pop());
			}
		};
	}

	/**
	 * Print to stdout. All arguments will be concatenated with a blank space.
	 */
	public static Process echo(String... args) {
		return new Process(args) {
			@Override
			public void runme() throws Exception {
				for (String s : expArgs())
					stdout.print(s + " ");
				stdout.println();
			}
		};
	}

	/**
	 * List directory. Do not print hidden files.
	 */
	public static Process ls(String... args) {
		return new Process(args) {
			@Override
			public void runme() throws FileNotFoundException {
				if (expArgs().isEmpty())
					expArgs().add(".");

				for (String arg : expArgs()) {
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
	 * Copy files. Many regular (non-directory) files may be copied into a
	 * single target directory. Destination must be a directory, and must not
	 * exist.
	 * 
	 * We allow one only exception to the general rule: if exactly one source
	 * file is given, and the destination target does not exist, and the
	 * <i>parent</i> of the target is an existing directory, than we guess that
	 * the user means to create a new regular file with the given name.
	 * 
	 * @param sourcesAndDest
	 *            At least one source file or folder, and exactly one
	 *            destination folder.
	 */
	public static Process cp(String... sourcesAndDest) {
		return new Process(sourcesAndDest) {
			@Override
			public void runme() throws IOException {
				if (expArgs().size() < 2)
					throw new IllegalArgumentException(
							"cp requires at least one source and the destination");

				File destDir = new File(getAbsolutePath(expArgs().get(
						expArgs().size() - 1)));

				if (expArgs().size() == 2 && !destDir.exists()
						&& destDir.getParentFile().isDirectory()) {
					// different algorithm: in this case, we just copy the file
					// with a new name
					File src = new File(getAbsolutePath(expArgs().get(0)));
					copy(src, destDir);

				} else {

					if (!destDir.isDirectory())
						throw new IllegalArgumentException(destDir.getPath()
								+ " does not exist or is not a folder");

					for (int i = 0; i < expArgs().size() - 1; ++i) {
						File src = new File(getAbsolutePath(expArgs().get(i)));
						if (src.isDirectory())
							throw new IllegalArgumentException(src.getPath()
									+ " is a directory");
						File dest = new File(destDir, src.getName());
						if (dest.exists())
							throw new IllegalArgumentException(dest.getPath()
									+ " already exists");
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
	 * Copy files and folders recursively. Many regular files and/or directories
	 * may be copied into a single target directory. Destination must be a
	 * directory, and must not exist.
	 * 
	 * We allow one only exception to the general rule: if exactly one source
	 * file/directory is given, and the destination target does not exist, and
	 * the <i>parent</i> of the target is an existing directory, than we guess
	 * that the user means to create a new regular file with the given name.
	 * 
	 * @param sourcesAndDest
	 *            At least one source file or folder, and exactly one
	 *            destination folder.
	 */
	public static Process cp_r(String... sourcesAndDest) {
		return new Process(sourcesAndDest) {
			@Override
			public void runme() throws IOException {
				if (expArgs().size() < 2)
					throw new IllegalArgumentException(
							"cp requires at least one source and the destination");

				File destDir = new File(getAbsolutePath(expArgs().get(
						expArgs().size() - 1)));

				if (expArgs().size() == 2 && !destDir.exists()
						&& destDir.getParentFile().isDirectory()) {
					// different algorithm: in this case, we just copy the file
					// with a new name
					File src = new File(getAbsolutePath(expArgs().get(0)));
					copyR(src, destDir);

				} else {

					if (!destDir.isDirectory())
						throw new IllegalArgumentException(destDir.getPath()
								+ " does not exist or is not a folder");

					for (int i = 0; i < expArgs().size() - 1; ++i) {
						File src = new File(getAbsolutePath(expArgs().get(i)));
						File dest = new File(destDir, src.getName());
						if (dest.exists())
							throw new IllegalArgumentException(dest.getPath()
									+ " already exists");
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
	 * Move files and folders to a new location. Many regular files and/or
	 * directories may be moved into a single target directory. Destination must
	 * be a directory, and must not exist.
	 * 
	 * We allow one only exception to the general rule: if exactly one source
	 * file/directory is given, and the destination target does not exist, and
	 * the <i>parent</i> of the target is an existing directory, than we guess
	 * that the user means to rename the source.
	 * 
	 * @param sourcesAndDest
	 *            At least one source file or folder, and exactly one
	 *            destination folder.
	 */
	public static Process mv(String... sourcesAndDest) {
		return new Process(sourcesAndDest) {
			@Override
			public void runme() {
				if (expArgs().size() < 2)
					throw new IllegalArgumentException(
							"mv requires at least one source and the destination");

				File destDir = new File(getAbsolutePath(expArgs().get(
						expArgs().size() - 1)));

				if (expArgs().size() == 2 && !destDir.exists()
						&& destDir.getParentFile().isDirectory()) {
					// different algorithm: in this case, we just rename the
					// folder
					File src = new File(getAbsolutePath(expArgs().get(0)));
					src.renameTo(destDir);

				} else {

					if (!destDir.isDirectory())
						throw new IllegalArgumentException(destDir.getPath()
								+ " does not exist or is not a folder");

					for (int i = 0; i < expArgs().size() - 1; ++i) {
						File src = new File(getAbsolutePath(expArgs().get(i)));

						File dest = new File(destDir, src.getName());
						if (dest.exists())
							throw new IllegalArgumentException(dest.getPath()
									+ " already exists");

						src.renameTo(dest);
					}
				}
			}
		};
	}

	/**
	 * Remove regular (non/directory) files. No error is given if file does not
	 * exists. An error is given if file is a directory.
	 */
	public static Process rm(String... src) {
		return new Process(src) {
			@Override
			public void runme() {
				for (String s : expArgs()) {
					File f = new File(getAbsolutePath(s));
					if (!f.exists())
						continue;
					if (f.isDirectory())
						throw new IllegalArgumentException(f.getPath()
								+ " is a directory");
					f.delete();
				}
			}
		};
	}

	/**
	 * Remove recursively files and folders. No error is given if file does not
	 * exists.
	 */
	public static Process rm_r(String... src) {
		return new Process(src) {
			@Override
			public void runme() {
				for (String s : expArgs()) {
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
	 * Make directory.
	 */
	public static Process mkdir(String... folders) {
		return new Process(folders) {
			@Override
			public void runme() {
				for (String s : expArgs())
					new File(getAbsolutePath(s)).mkdir();
			}
		};
	}

	/**
	 * Make directory including all upper levels.
	 */
	public static Process mkdir_p(String... folders) {
		return new Process(folders) {
			@Override
			public void runme() {
				for (String s : expArgs())
					new File(getAbsolutePath(s)).mkdirs();
			}
		};
	}

	/**
	 * Remove empty directories. No error is given if folder does not exists. An
	 * error is given if file is not a directory, or the directory is not empty.
	 */
	public static Process rmdir(String... folders) {
		return new Process(folders) {
			@Override
			public void runme() {
				for (String s : expArgs()) {
					File f = new File(getAbsolutePath(s));
					if (!f.exists())
						continue;
					if (!f.isDirectory())
						throw new IllegalArgumentException(f.getPath()
								+ " is not a directory");
					if (f.list().length > 0) {
						// FIXME does not catch hidden files
						throw new IllegalArgumentException(f.getPath()
								+ " is not empty");
					}
					f.delete();
				}
			}
		};
	}

	/**
	 * Create hard link. If no destination is given, current folder will be
	 * used. Destination may be an existing folder, or a non-existing regular
	 * file name.
	 * 
	 * Not all operating systems may support links, and not for all kind of
	 * files.
	 * 
	 * @param src
	 *            Exactly one source (i.e. target) file, and zero or one
	 *            destinations.
	 */
	public static Process ln(String... srcAndDest) {
		return new Process(srcAndDest) {
			@Override
			public void runme() throws IOException {

				if (expArgs().isEmpty() || expArgs().size() > 2)
					throw new IllegalArgumentException(
							"1 or 2 arguments expected");

				File target = new File(getAbsolutePath(expArgs().get(0)));
				File link = new File(
						(expArgs().size() == 1) ? getAbsolutePath(".")
								: getAbsolutePath(expArgs().get(1)));

				if (!target.exists())
					throw new IllegalArgumentException(
							"Target file does not exist: " + target.getPath());

				if (link.exists()) {
					if (link.isDirectory())
						link = new File(link, target.getName());
					else
						throw new IllegalArgumentException(
								"Link file already exist: " + link.getPath());
				}

				Files.createLink(Paths.get(link.getPath()),
						Paths.get(target.getPath()));
			}
		};
	}

	/**
	 * Create symbolic link. If no destination is given, current folder will be
	 * used. Destination may be an existing folder, or a non-existing regular
	 * file name.
	 * 
	 * Not all operating systems may support links, and not for all kind of
	 * files.
	 * 
	 * @param src
	 *            Exactly one source (i.e. target) file, and zero or one
	 *            destinations.
	 */
	public static Process ln_s(String... srcAndDest) {
		return new Process(srcAndDest) {

			@Override
			public void runme() throws IOException {

				if (expArgs().isEmpty() || expArgs().size() > 2)
					throw new IllegalArgumentException(
							"1 or 2 arguments expected");

				File target = new File(getAbsolutePath(expArgs().get(0)));
				File link = new File(
						(expArgs().size() == 1) ? getAbsolutePath(".")
								: getAbsolutePath(expArgs().get(1)));

				if (!target.exists())
					throw new IllegalArgumentException(
							"Target file does not exist: " + target.getPath());

				if (link.exists()) {
					if (link.isDirectory())
						link = new File(link, target.getName());
					else
						throw new IllegalArgumentException(
								"Link file already exist: " + link.getPath());
				}

				Files.createSymbolicLink(Paths.get(link.getPath()),
						Paths.get(target.getPath()));
			}
		};
	}

	/**
	 * Concatenate files and print to standard output.
	 */
	public static Process cat(String... src) {
		return new Process(src) {
			@Override
			public void runme() throws IOException {

				List<BufferedReader> sources = this.getReaders(expArgs());
				for (BufferedReader r : sources) {
					String line;
					while ((line = r.readLine()) != null) {
						stdout.println(line);
					}
				}
			}
		};
	}

	/**
	 * Print text lines matching a pattern.
	 */
	public static Process grep(final String text, String... src) {
		return new Process(src) {
			@Override
			public void runme() throws IOException {

				List<BufferedReader> sources = this.getReaders(expArgs());
				for (BufferedReader r : sources) {
					String line;
					while ((line = r.readLine()) != null) {
						if (line != null && line.contains(text))
							stdout.println(line);
					}
				}
			}
		};
	}

	/**
	 * Print text lines <i>not</i> matching a pattern.
	 */
	public static Process grep_v(final String text, String... src) {
		return new Process(src) {
			@Override
			public void runme() throws IOException {

				List<BufferedReader> sources = this.getReaders(expArgs());
				for (BufferedReader r : sources) {
					String line;
					while ((line = r.readLine()) != null) {
						if (line != null && !line.contains(text))
							stdout.println(line);
					}
				}
			}
		};
	}

	/**
	 * Always succeed.
	 */
	public static Process true_() {
		return new Process() {
			@Override
			public void runme() {

			}
		};
	}

	/**
	 * Fail always.
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
	 * Search for files in a directory hierarchy.
	 */
	public static Process find(String options, String folder) {
		throw new IllegalStateException("Not implemented");
	}

	/**
	 * Download files from network. Output to stdout.
	 * 
	 * @throws IOException
	 */
	public static Process wget(final String address) {
		// FIXME this is not the standard behaviour: wget alone does /not/ print
		// to stdout
		return new Process() {
			@Override
			public void runme() throws IOException {
				URL website = new URL(address);
				BufferedInputStream in = null;
				try {
					in = new BufferedInputStream(website.openStream());

					final byte data[] = new byte[1024];
					int count;
					while ((count = in.read(data, 0, 1024)) != -1) {
						stdout.write(data, 0, count);
					}
				} finally {
					if (in != null) {
						in.close();
					}
				}
			}
		};
	}

	/**
	 * Download files from network and save output file with given name.
	 * 
	 * @throws IOException
	 */
	public static Process wget(String address, String localFile)
			throws IOException {
		return wget(address).redirect(localFile);
	}

	/**
	 * Add/update files to ZIP archive. TODO.
	 */
	public static Process zip(String args) {
		throw new IllegalStateException("Not implemented");
	}

	/**
	 * Extract files from ZIP archive. TODO.
	 */
	public static Process unzip(String args) {
		throw new IllegalStateException("Not implemented");
	}

	/**
	 * Add/update files to TAR/TGZ archive. TODO.
	 */
	public static Process tar_c(String args) {
		throw new IllegalStateException("Not implemented");
	}

	/**
	 * Extract files from TAR/TGZ archive. TODO.
	 */
	public static Process tar_x(String args) {
		throw new IllegalStateException("Not implemented");
	}
}
