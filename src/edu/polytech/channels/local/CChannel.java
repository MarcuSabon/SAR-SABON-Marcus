package edu.polytech.channels.local;

import edu.polytech.channels.Channel;
import edu.polytech.utils.CircularBuffer;

public class CChannel implements Channel {

	private final CircularBuffer buffer = new CircularBuffer(1024);

    private CChannel remote;
    private volatile boolean localDisconnected = false;
    private volatile boolean remoteDisconnected = false;
    
    protected CChannel() {
    }
  
  protected void setRemote(CChannel remoteChannel) {
	    this.remote = remoteChannel;
	}

  @Override
  public int read(byte[] bytes, int offset, int length) {
      if (offset < 0 || length < 0 || offset + length > bytes.length) {
          throw new IllegalArgumentException("Probleme d'offset ou de longueur");
      }
      if (length == 0) return 0;

      synchronized (this) {          
    	  while (buffer.empty() && !localDisconnected && !remoteDisconnected) {
              try {
                  wait();
              } catch (InterruptedException e) {
                  Thread.currentThread().interrupt();
                  return 0;
              }
          }

          if (buffer.empty() && (localDisconnected || remoteDisconnected)) {
              return 0;
          }

          int readCount = 0;
          while (readCount < length && !buffer.empty()) {
              bytes[offset + readCount] = buffer.pull();
              readCount++;
          }
          
          
          notifyAll(); 
          return readCount;
      }
  }

  @Override
  public int write(byte[] bytes, int offset, int length) {
	
      if (offset < 0 || length < 0 || offset + length > bytes.length) {
          throw new IllegalArgumentException("Probleme d'offset ou de longueur");
      }
      if (length == 0) return 0;

      if (localDisconnected) {
          return length; 
      }

      
      synchronized (remote) {
      
          while (remote.buffer.full() && !this.localDisconnected && !remote.localDisconnected) {
              try {
                  remote.wait(); 
              } catch (InterruptedException e) {
                  Thread.currentThread().interrupt();
                  return 0;
              }
          }

        
          if (this.localDisconnected || remote.localDisconnected) {
              return length;
          }

          int writeCount = 0;
          while (writeCount < length && !remote.buffer.full()) {
              remote.buffer.push(bytes[offset + writeCount]);
              writeCount++;
          }
          
          remote.notifyAll(); 
          return writeCount;
      }
  }

  @Override
  public boolean disconnected() {
	  synchronized (this) {
	  return localDisconnected || (remoteDisconnected && buffer.empty());
	  }
  }

  @Override
  public void disconnect() {
	    synchronized (this) {
	        if (localDisconnected) return;
	        localDisconnected = true;
	        notifyAll();
	    }
	    if (remote != null) {
	        synchronized (remote) {
	            remote.remoteDisconnected = true;
	            remote.notifyAll();
	        }
	    }
  }

}
