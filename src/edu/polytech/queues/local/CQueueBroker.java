package edu.polytech.queues.local;

import java.util.HashMap;

import edu.polytech.queues.QueueBroker;
import edu.polytech.queues.Task;
import edu.polytech.utils.Executor;

public class CQueueBroker implements QueueBroker {

	private String name;
	private Task task;
	private HashMap<Integer, BindListener> bindings = new HashMap<>();
	private static HashMap<String, CQueueBroker> brokers = new HashMap<>();

	public CQueueBroker(String name) {
		Executor.check();
		if (name == null) {
			throw new IllegalArgumentException("Broker name is null");
		}
		if (brokers.containsKey(name)) {
			throw new IllegalArgumentException("Broker name already in use: " + name);
		}
		this.name = name;
		this.task = Task.task();
		brokers.put(name, this);

	}
	
	@Override
	public String getName() {
		return name;
	}

	@Override
	public Task getTask() {
		return task;
	}

	@Override
	public boolean bind(int port, BindListener listener) {
		Executor.check();
		if (listener == null) {
			throw new IllegalArgumentException("null listener");
		}
		if (bindings.containsKey(port))
			return false;
		bindings.put(port, listener);
		return true;
	}

	@Override
	public boolean unbind(int port) {
		Executor.check();
		BindListener l = bindings.remove(port);
		if (l == null) {
			return false;
		}
		task.post(new Runnable() {
			public void run() {
				l.unbound();
			}
		});
		return true;
	}

	@Override
	public boolean connect(String name, int port, ConnectListener listener) {
		Executor.check();
		if (name == null || listener == null) {
			throw new IllegalArgumentException("null argument");
		}
		CQueueBroker remote = brokers.get(name);
		if (remote == null) {
			return false;
		}
		task.post(new Runnable() {
			public void run() {
				BindListener bl = remote.bindings.get(port);
				if (bl == null || remote.task.dead()) {
					listener.refused();
					return;
				}

				CMessageQueue local = new CMessageQueue(CQueueBroker.this);
				CMessageQueue peer = new CMessageQueue(remote);

				CMessageQueue.link(local, peer);

				remote.task.post(new Runnable() {
					public void run() {
						bl.accepted(peer);
					}
				});
				listener.connected(local);
			}
		});
		return true;
	}

}
