package javax.shell.test;

import java.util.List;

import javax.shell.Process;

/**
 * A {@see Process} that emits Three strings.
 * 
 * @author vercelli 2016
 *
 */
public class TesterProcessNoInput extends Process {

	List<Integer> processesRan = null;
	Integer id = null;

	public TesterProcessNoInput(int id, List<Integer> processesRan) {
		this.processesRan = processesRan;
		this.id = id;
	}

	@Override
	public void runme() {
		processesRan.add(id);
		stdout.println("ehlo");
		stdout.println("mydarling");
		stdout.println("seeyou");
	}

}
