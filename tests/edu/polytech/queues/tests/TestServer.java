/*
 * Copyright (C) 2026 Pr. Olivier Gruber                                    
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
package edu.polytech.queues.tests;

import edu.polytech.queues.Task;
import edu.polytech.queues.local.CMessageQueue;
import edu.polytech.queues.local.CQueueBroker;
import edu.polytech.queues.local.CTask;

public class TestServer {
  static boolean VERBOSE = true;

  static void log(String s) {
    if (VERBOSE)
      System.out.println(s);
  }

  static void log(String s, Throwable th) {
    if (VERBOSE) {
      System.out.println(s);
      th.printStackTrace();
    }
  }

  String name;
  int port;
  int cno = 0;
  CQueueBroker broker;
  CTask task;
  
  TestServer(String name, int port) {
    this.name = name;
    this.port = port;
    this.task = (CTask) CTask.task().newTask(name);
    this.task.post(()->{
      log("Server "+name+" started!");
      this.broker = task.newBroker(name);
      bind();
    });

  }

  private void bind() {
    broker.bind(port, new CQueueBroker.BindListener() {

      @Override
      public void accepted(CMessageQueue queue) {
        log("Server "+name+" accepted a connection...");
        new ClientSession(queue, cno++);
      }

      @Override
      public void unbound() {
        log("Server: "+name+" unbound port=" + port);
      }

    });
  }

  class QueueListener implements CMessageQueue.Listener {
    CMessageQueue queue;
    int no;
    CTask task;
    QueueListener(CTask task, CMessageQueue queue, int no) {
      this.task = task;
      this.queue = queue;
      this.no = no;
    }
    
    @Override
    public void received(byte[] msg) {
      queue.send(msg, 0, msg.length, new CMessageQueue.SendListener() {

        @Override
        public void sent(byte[] bytes, int offset, int length) {
          log("Server: worker[" + no + "] sent " + msg.length + " bytes!");
        }
      });
      log("Server: worker[" + no + "] echoed " + msg.length + " bytes!");
    }

    @Override
    public void closed() {
      log("Server: worker[" + no + "] closed connection.");
      assert(task==CTask.task());
      task.exit(null);
    }  
  }
  
  class ClientSession implements CTask.Listener {
    String name;
    CMessageQueue queue;
    int no;
    CTask task;
    ClientSession(CMessageQueue mq, int no) {
      this.queue = mq;
      this.no = no;
      this.name = "Server["+broker.getName() + "]:Worker[" + no + "]";
      CTask t = (CTask) CTask.task();
      this.task = t.newTask(name);
      this.task.set(this);
      this.task.post(() -> {
        mq.setListener(new QueueListener(task,mq,no));
      });
    }
    @Override
    public void failed(Task t, Throwable th) {
      log(name+" failed.",th);
    }
    @Override
    public void completed(Task t, Object o) {
      log(name+" completed.");
    }
  }

}
