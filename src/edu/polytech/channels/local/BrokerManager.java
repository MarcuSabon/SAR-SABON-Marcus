package edu.polytech.channels.local;

import java.util.HashMap;
import java.util.Map;

public class BrokerManager {

	private final Map<String, CBroker> brokers = new HashMap<>();


  BrokerManager() {
  }

  public void add(CBroker broker) {
	  if (broker != null && broker.getName() != null) {
          brokers.put(broker.getName(), broker);
      }
  }
  
  public void remove(CBroker broker) {
	  if (broker != null && broker.getName() != null) {
          brokers.remove(broker.getName());
      }
  }
  
  public CBroker get(String name) {
	  return brokers.get(name);
  }
  
}
