package one.zookeeper.lock;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class ProtocolSupport {

    private static final Logger log = LoggerFactory.getLogger(ProtocolSupport.class);
    private static final int RETRY_COUNT = 30;

    protected final ZooKeeper zookeeper;
    private AtomicBoolean closed = new AtomicBoolean(false);
    private long retryDelay = 500L;
    private List<ACL> acl = ZooDefs.Ids.OPEN_ACL_UNSAFE;

    public ProtocolSupport(ZooKeeper zookeeper) {
        this.zookeeper = zookeeper;
    }

    public void close() {
        if (closed.compareAndSet(false, true)) {
            doClose();
        }
    }

    public ZooKeeper getZookeeper() {
        return zookeeper;
    }

    public List<ACL> getAcl() {
        return acl;
    }

    public void setAcl(List<ACL> acl) {
        this.acl = acl;
    }

    public long getRetryDelay() {
        return retryDelay;
    }

    public void setRetryDelay(long retryDelay) {
        this.retryDelay = retryDelay;
    }

    public void doClose() {
        throw new UnsupportedOperationException("not support doClose");
    }

    protected boolean retryOperation(ZookeeperOperation operation)
            throws KeeperException, InterruptedException {
        KeeperException exception = null;
        for (int i = 0; i < RETRY_COUNT; i++) {
            try {
                return operation.execute();
            } catch (KeeperException.SessionExpiredException e) {
                log.warn("Session expired {}. Reconnecting...", zookeeper, e);
                throw e;
            } catch (KeeperException.ConnectionLossException e) {
                if (exception == null) {
                    exception = e;
                }
                log.debug("Attempt {} failed with connection loss. Reconnecting...", i);
                retryDelay(i);
            }
        }
        throw exception;
    }

    protected void retryDelay(int attemptCount) {
        if (attemptCount > 0) {
            try {
                Thread.sleep(attemptCount * retryDelay);
            } catch (InterruptedException e) {
                log.warn("Failed to sleep.", e);
            }

        }
    }

    protected boolean isClosed() {
        return closed.get();
    }

    protected void ensureExists(
            final String path,
            final byte[] data,
            final List<ACL> acl,
            final CreateMode flags) {
        try {
            retryOperation(() -> {
                Stat stat = zookeeper.exists(path, false);
                if (stat != null) {
                    return true;
                }
                zookeeper.create(path, data, acl, flags);
                return true;
            });
        } catch (KeeperException | InterruptedException e) {
            log.warn("Unexpected exception", e);
        }
    }

    protected void ensurePathExists(String path) {
        ensureExists(path, null, acl, CreateMode.PERSISTENT);
    }
}
