package edu.polytech.queues.local;

import edu.polytech.queues.Bootstrap;
import edu.polytech.queues.Task;
import edu.polytech.utils.Executor;

public class Boot implements Bootstrap{

	public Boot() {
		
	}
	
	@Override
	public Task newTask(Runnable r, String name) {
		Executor pump = Executor.self();
		Task t = pump.newTask(name);
		
		synchronized(pump) {
			t.post(r);
		}
		return t;
	}

}

