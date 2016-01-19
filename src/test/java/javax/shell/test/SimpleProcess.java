package javax.shell.test;

import java.util.List;

import javax.shell.Process;

public class SimpleProcess extends Process {

	List<Integer> processesRan = null;
	Integer id = null;
	List<String> linesReceived = null;

	public SimpleProcess(int id, List<Integer> processesRan, List<String> linesReceived) {
		this.processesRan = processesRan;
		this.id = id;
		this.linesReceived = linesReceived;
	}

	@Override
	public void runme() throws Exception {
		while (stdinReader.ready()) {
			linesReceived.add(stdinReader.readLine());
		}
		processesRan.add(id);
	}

}
