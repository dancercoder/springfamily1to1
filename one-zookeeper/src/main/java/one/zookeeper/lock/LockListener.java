package one.zookeeper.lock;

public interface LockListener {

    void lockAcquired();

    void lockReleased();
}
