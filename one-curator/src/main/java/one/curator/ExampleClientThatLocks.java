package one.curator;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessLock;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;

public class ExampleClientThatLocks {
    private final InterProcessLock lock;
    private final MockLimitedResource resource;
    private final String clientName;

    public ExampleClientThatLocks(CuratorFramework client,
                                  String lockPath,
                                  MockLimitedResource resource,
                                  String clientName) {
        this.resource = resource;
        this.clientName = clientName;
        lock = new InterProcessMutex(client, lockPath);
    }
}
