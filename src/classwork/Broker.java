package classwork;

public abstract class Broker {
    Broker(String name) {
    };

    Channel accept(int port) {
        return null;
    };

    Channel connect(String name, int port) {
        return null;
    };
}
