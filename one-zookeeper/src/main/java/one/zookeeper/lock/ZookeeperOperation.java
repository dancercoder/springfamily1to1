package one.zookeeper.lock;

import org.apache.zookeeper.KeeperException;

public interface ZookeeperOperation {

    boolean execute()throws KeeperException,InterruptedException;
}
