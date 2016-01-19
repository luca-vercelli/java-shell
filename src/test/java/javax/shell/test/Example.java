package javax.shell.test;

import static javax.shell.Unix.*;

public class Example {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		
		String x = "fi*le*ff?le";
		System.out.println(x.replace("*","\\*").replace("?","\\?"));
		System.out.println(x.replaceAll("\\*","\\\\*").replaceAll("\\?","\\\\?"));
		
		
		System.exit(-1);
		ls("*").pipe(grep("carlo"));
		

	}

}
