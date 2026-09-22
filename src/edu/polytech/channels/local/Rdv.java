package edu.polytech.channels.local;

import edu.polytech.channels.Channel;

public class Rdv {
    
    private int waitingAccepts = 0;
    private CChannel channelForAccept = null;

    public synchronized Channel accept() {
        waitingAccepts++;
        notifyAll();

        while (channelForAccept == null) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }

        CChannel result = channelForAccept;
        channelForAccept = null; 
        notifyAll();
        return result;
    }

    public synchronized Channel connect() {
        while (waitingAccepts == 0) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }

        waitingAccepts--;

        CChannel acceptSide = new CChannel();
        CChannel connectSide = new CChannel();
        
        acceptSide.setRemote(connectSide);
        connectSide.setRemote(acceptSide);

        channelForAccept = acceptSide;
        notifyAll();

        while (channelForAccept != null) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        return connectSide;
    }
}