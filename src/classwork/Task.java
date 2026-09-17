package classwork;

public abstract class Task extends Thread {
    Task(Broker b, Runnable r) {
    };

    static Broker getBroker() {
        return null;
    };
}