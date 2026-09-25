/*
 * Copyright (C) Pr. Olivier Gruber                                    
 *                                                                       
 * This program is free software: you can redistribute it and/or modify  
 * it under the terms of the GNU General Public License as published by  
 * the Free Software Foundation, either version 3 of the License, or     
 * (at your option) any later version.                                   
 *                                                                       
 * This program is distributed in the hope that it will be useful,       
 * but WITHOUT ANY WARRANTY; without even the implied warranty of        
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         
 * GNU General Public License for more details.                          
 *                                                                       
 * You should have received a copy of the GNU General Public License     
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */
package edu.polytech.queues.local;

import edu.polytech.queues.MessageQueue;
import edu.polytech.queues.local.CQueueBroker;

import java.util.Arrays;
import java.util.LinkedList;

import edu.polytech.queues.MessageQueue;
import edu.polytech.queues.QueueBroker;
import edu.polytech.queues.Task;
import edu.polytech.utils.Executor;


public class CMessageQueue implements MessageQueue {

	private CQueueBroker broker;
	private CMessageQueue peer;

	private LinkedList<byte[]> pending = new LinkedList<>();
	private Listener listener;
	private Task listenerTask;

	private boolean closed;
	private boolean peerClosed;
	private boolean closedNotified;

	CMessageQueue(CQueueBroker broker) {
		this.broker = broker;
	}

	static void link(CMessageQueue a, CMessageQueue b) {
		a.peer = b;
		b.peer = a;
	}

	@Override
	public QueueBroker broker() {
		return broker;
	}

	@Override
	public void setListener(Listener l) {
		Executor.check();
		listener = l;
		if (l == null) {
			listenerTask = null;
			return;
		}
		listenerTask = Task.task();
		Executor.self().register(listenerTask, this);
		while (!pending.isEmpty())
			receive(pending.removeFirst());
		maybeNotifyClosed();
	}

	@Override
	public boolean send(byte[] bytes, int offset, int length, SendListener sl) {
		Executor.check();
		if (bytes == null || offset < 0 || length < 0 || offset + length > bytes.length)
			throw new IllegalArgumentException("bad range");
		boolean accepted = !(closed || peerClosed);

		if (accepted) {
			peer.receive(Arrays.copyOfRange(bytes, offset, offset + length));
		}

		if (sl != null) {
			Task.task().post(() -> sl.sent(bytes, offset, length));
		}

		return accepted;
	}

	private void receive(byte[] msg) {
		if (listener == null) {
			pending.addLast(msg);
			return;
		}
		Listener l = listener;
		listenerTask.post(() -> l.received(msg));
	}

	private void maybeNotifyClosed() {
		if (closedNotified || listener == null || !(closed || peerClosed))
			return;
		closedNotified = true;
		Listener l = listener;
		listenerTask.post(() -> {
			closed = true;
			l.closed();
		});
	}

	@Override
	public void close() {
		Executor.check();
		if (closed)
			return;
		closed = true;
		pending.clear();
		if (!peerClosed) {
			peer.peerClosed = true;
			peer.maybeNotifyClosed();
		}
		maybeNotifyClosed();
	}

	@Override
	public boolean closed() {
		return closed;
	}

}
