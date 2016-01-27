package javax.shell;

import java.util.HashMap;
import java.util.Map;

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

}
