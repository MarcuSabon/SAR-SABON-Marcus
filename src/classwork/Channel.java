package classwork;

public abstract class Channel {
    int read(byte[] bytes, int offset, int length) {
        return 0;
    };

    int write(byte[] bytes, int offset, int length) {
        return 0;
    };

    void disconnect() {
    };

    boolean disconnected() {
        return false;
    };
}
