package javax.shell;

public class DOS {
	
	// TODO
	
	/**
	 * DOS program <code>dir</code> (i.e. list directory).
	 */
	public static Process dir(String... args) {
		return Unix.ls(args);
	}

}
