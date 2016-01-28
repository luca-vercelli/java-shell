package javax.shell;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Pattern;

/**
 * The environment where {@see Process}es run. The user is supposed <i>not</i>
 * to call these methods directly.
 * 
 * How many Shell's are needed? We guess that one per Thread should be fine.
 * 
 * @author luca vercelli 2016
 * 
 */
public class Shell {

	private static Map<String, Shell> instances = new HashMap<String, Shell>();

	public static Shell getInstance() {
		String threadName = Thread.currentThread().getName();
		Shell sh = instances.get(threadName);
		if (sh == null) {
			sh = new Shell();
			instances.put(threadName, sh);
		}

		// DEBUG CODE
		// System.out.println("Returning Shell: " + sh);

		return sh;
	}

	public String getCurrentFolder() {
		return currentFolder;
	}

	public void setCurrentFolder(String currentFolder) {
		this.currentFolder = currentFolder;
	}

	public Map<String, String> getEnv() {
		return env;
	}

	public void setEnv(Map<String, String> env) {
		this.env = env;
	}

	public void set(String property, String value) {
		env.put(property, value);
	}

	public String get(String property) {
		return env.get(property);
	}

	/**
	 * Recall that in Java there is not good "current folder", so we store it
	 * here.
	 * 
	 * This must be an absolute path.
	 */
	private String currentFolder = System.getProperty("user.dir");

	/**
	 * Properties
	 */
	private Map<String, String> env = new HashMap<String, String>();

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
	 * Shell-expansion of arguments. Every argument is expanded according to
	 * current folder. If no expansion is available, the argument is returned
	 * as-is.
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
			// System.err.println("SPLITROOT path=" + path + " pieces="
			// + Arrays.asList(pieces) + " pwd=" + getCurrentFolder());

			File root = new File(pieces[0]);

			Stack<String> piecesStack = new Stack<String>();
			for (int i = pieces.length - 1; i > 0; --i)
				piecesStack.push(pieces[i]);

			Set<String> singlePathExpansion = new HashSet<String>();

			expandRecursive(root, piecesStack, singlePathExpansion);

			if (singlePathExpansion.isEmpty()) {
				ret.add(path);
			} else {
				ret.addAll(singlePathExpansion);
			}
		}
		return new ArrayList<String>(ret);
	}
}
