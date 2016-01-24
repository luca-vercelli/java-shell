package javax.shell.test;

import java.util.ArrayList;


public class Example {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		ArrayList<Integer> ran = new ArrayList<Integer>(); 
		TesterProcess p1 = new TesterProcess(1, ran);
		TesterProcess p2 = new TesterProcess(2, ran);
			
		p1.pipe(p2);

		p1.start();
		p2.start();
	}

}
