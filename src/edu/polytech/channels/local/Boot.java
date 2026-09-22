package edu.polytech.channels.local;

import edu.polytech.channels.Bootstrap;
import edu.polytech.channels.Broker;
import edu.polytech.channels.Task;

public class Boot implements Bootstrap {
	
	public static BrokerManager brokerManager;

  public Boot() {
	  brokerManager = new BrokerManager();
  }
  
  @Override
  public Broker newBroker(String name) {
	  CBroker broker = new CBroker(name);
	  brokerManager.add(broker);
      return broker;
  }

  @Override
  public Task newTask(Broker b, Runnable r, String name) {
	  return new CTask(b, r, name);
  }

}
