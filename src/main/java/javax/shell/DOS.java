package javax.shell;

/**
 * DOS prompt commands. TODO.
 * 
 * @author luca vercelli 2016
 */
public class DOS {

	/**
	 * DOS program <code>dir</code> (i.e. list directory).
	 */
	public static Process dir(String... args) {
		return Unix.ls(args);
	}

	/**
	 * DOS program <code>echo</code> (i.e. print to stdout).
	 */
	public static Process echo(String... args) {
		return Unix.echo(args);
	}

	/**
	 * DOS program <code>find</code> (i.e. find text inside files).
	 */
	public static Process find(String text, String... files) {
		return Unix.grep(text, files);
	}

}
