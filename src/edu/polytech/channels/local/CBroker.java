package edu.polytech.channels.local;

import java.util.HashMap;
import java.util.Map;
import edu.polytech.channels.Broker;
import edu.polytech.channels.Channel;

public class CBroker implements Broker {
	private final String name;
	private final Map<Integer, Rdv> ports = new HashMap<>();
  CBroker(String name) {
	  this.name = name;
  }

  @Override
  public String getName() {
	  return name;
  }

  @Override
  public Channel connect(String name, int port) {
	  CBroker remoteBroker = Boot.brokerManager.get(name);
	  if (remoteBroker == null) {
          return null;
      }
	  Rdv rdv = remoteBroker.getRdv(port);
	  return rdv.connect();
  }

  @Override
  public Channel accept(int port) {
	  Rdv rdv = getRdv(port);
	  return rdv.accept();
  }
  synchronized Rdv getRdv(int port) {
      Rdv rdv = ports.get(port);
      if (rdv == null) {
          rdv = new Rdv();
          ports.put(port, rdv);
      }
      return rdv;
  }
}
