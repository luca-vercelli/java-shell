package javax.shell.test;

import static javax.shell.Unix.*;

import java.io.File;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

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
