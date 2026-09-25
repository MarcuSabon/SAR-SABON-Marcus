package edu.polytech.queues.local;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import edu.polytech.queues.Task;
import edu.polytech.queues.local.CMessageQueue;
import edu.polytech.queues.local.CQueueBroker;
import edu.polytech.utils.Executor;

public class CTask extends Task {
  Executor e;
  CQueueBroker broker;
  String name;
  boolean failed, completed;
  Listener l;
  CTask tl;
  Object result;
  Throwable cause;

  public CTask(String name) {
    this.name = name;
    this.e = Executor.self();
  }

  @Override 
  public String toString() {
    return "Task "+name;
  }
  
  public void set(CQueueBroker qb) {
    if (broker != null)
      throw new IllegalStateException("Task " + name + " has a broker already.");
    broker = qb;
  }

  @Override
  public void post(Runnable r) {
    if (!dead())
      e.post(this, r);
  }

  @Override
  public void post(Runnable r, int delay) {
    if (!dead())
      e.post(this, r, delay);
  }

  @Override
  public CQueueBroker getBroker() {
    return broker;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public boolean dead() {
    return failed || completed;
  }

  @Override
  public CTask newTask(String name) {
    return new CTask(name);
  }

  @Override
  public CQueueBroker newBroker(String name) {
    return new CQueueBroker(name);
  }

  @Override
  public void set(Listener l) {
    if (failed || completed)
      throw new IllegalStateException("Dead task");
    this.tl = (CTask) CTask.task();
    this.l = l;
  }

  @Override
  public void fail(Throwable th) {
    if (failed)
      throw new Error("PANIC: failed task");
    failed = true;
    cause = th;
    done();
  }

  @Override
  public void exit(Object o) {
    if (failed || completed)
      throw new IllegalStateException("Dead task");
    completed = true;
    result = o;
    done();
  }

  /*
   * Really important to have this method because it is possible that a runnable
   * on this task invoked exit, but failed afterwards.
   */
  private void done() {
    Iterator<CMessageQueue> i = queues.iterator();
    while (i.hasNext()) {
      CMessageQueue mq = i.next();
      mq.close();
    }
    Listener l = this.l;
    if (l != null) {
      tl.post(() -> {
        if (failed)
          l.failed(this, cause);
        else
          l.completed(this, result);
      });
    } else {
      if (failed)
        Executor.log("Failed task "+name, cause);
    }
  }

  List<CMessageQueue> queues = new LinkedList<>();

  public void register(CMessageQueue mq) {
    queues.add(mq);
  }
}
