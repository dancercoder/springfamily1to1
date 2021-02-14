package one.zookeeper.lock;

import com.sun.org.apache.xpath.internal.operations.Bool;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

public class WriteLock extends ProtocolSupport {

    private static final Logger log = LoggerFactory.getLogger(WriteLock.class);

    private final String dir;
    private String id;
    private ZNodeName idName;
    private String ownerId;
    private String lastChildId;
    private byte[] data = {0x12, 0x34};
    private LockListener callback;
    private ZookeeperOperation zop;


    public WriteLock(ZooKeeper zooKeeper, String dir, List<ACL> acl) {
        super(zooKeeper);
        this.dir = dir;//dir应该是持久节点
        if (null != acl) {
            setAcl(acl);
        }

        this.zop = new ZookeeperOperation() {
            @Override
            public boolean execute() throws KeeperException, InterruptedException {
                do {
                    if (null == id) {
                        long sessionId = zooKeeper.getSessionId();
                        String prefix = "x-" + sessionId + "-";
                        findPrefixInChildren(prefix, zooKeeper, dir);
                        idName = new ZNodeName(id);
                    }
                    List<String> names = zooKeeper.getChildren(dir, false);
                    if (names.isEmpty()) {
                        log.warn("No children in: {} when we've just created one! Lets recreate it...", dir);
                        id = null;
                    } else {
                        SortedSet<ZNodeName> sortedNames = new TreeSet<>();
                        for (String name : names) {
                            sortedNames.add(new ZNodeName(dir + "/" + name));
                        }
                        ownerId = sortedNames.first().getName();//锁的拥有者
                        SortedSet<ZNodeName> lessThanMe = sortedNames.headSet(idName);
                        if (!lessThanMe.isEmpty()) {//如果当前节点是序号最小的节点
                            ZNodeName lastChildName = lessThanMe.last();
                            lastChildId = lastChildName.getName();
                            log.debug("Watching less than me node: {}", lastChildId);
                            Stat stat = zooKeeper.exists(lastChildId, new LockWatcher());//监听前序的节点，而不是监听所有的节点，防止羊群效应
                            if (null != stat) {
                                return Boolean.FALSE;
                            } else {
                                log.warn("Could not find the stats for less than me: {}", lastChildName.getName());
                            }
                        } else {
                            if (isOwner()) {
                                LockListener lockListener = getLockListener();
                                if (lockListener != null) {
                                    lockListener.lockAcquired();
                                }
                                return Boolean.TRUE;
                            }
                        }
                    }

                } while (id == null);
                return false;
            }

            private void findPrefixInChildren(String prefix, ZooKeeper zookeeper, String dir)
                    throws KeeperException, InterruptedException {
                List<String> names = zooKeeper.getChildren(dir, false);
                for (String name : names) {
                    if (name.startsWith(prefix)) {
                        id = name;
                        log.debug("Found id created last time: {}", id);
                        break;
                    }
                }
                if (null == id) {
                    //申请锁的时候，在dir下创建临时有序节点
                    id = zooKeeper.create(dir + "/" + prefix, data, getAcl(), CreateMode.EPHEMERAL_SEQUENTIAL);
                    log.debug("Create id: {}", id);
                }
            }
        };
    }

    public WriteLock(
            ZooKeeper zooKeeper,
            String dir,
            List<ACL> acl,
            LockListener callback) {
        this(zooKeeper, dir, acl);
        this.callback = callback;
    }

    public synchronized boolean lock() throws KeeperException, InterruptedException {
        if (isClosed()) {
            return false;
        }
        ensurePathExists(dir);
        return retryOperation(zop);
    }

    public synchronized void unlock() throws RuntimeException {
        if (!isClosed() && null != id) {
            try{
                ZookeeperOperation zopdel=new ZookeeperOperation() {
                    @Override
                    public boolean execute() throws KeeperException, InterruptedException {
                        zookeeper.delete(id,-1);
                        return Boolean.TRUE;
                    }
                };
                zopdel.execute();
            }catch (InterruptedException e){
                log.warn("Unexpected exception", e);
                Thread.currentThread().interrupt();
            }catch (KeeperException.NoNodeException e){

            }catch (KeeperException e){
                log.warn("Unexpected exception", e);
                throw new RuntimeException(e.getMessage(),e);
            }finally {
                LockListener lockListener = getLockListener();
                if (lockListener != null) {
                    lockListener.lockReleased();
                }
                id = null;
            }
        }
    }

    private class LockWatcher implements Watcher {
        @Override
        public void process(WatchedEvent event) {
            log.debug("Watcher fired: {}", event);
            try {
                lock();
            } catch (Exception e) {
                log.warn("Failed to acquire lock", e);
            }
        }
    }

    public boolean isOwner() {
        return id != null && id.equals(ownerId);
    }

    public synchronized LockListener getLockListener() {
        return this.callback;
    }

    public synchronized void setLockListener(LockListener callback) {
        this.callback = callback;
    }

    public String getDir() {
        return dir;
    }

    public String getId() {
        return this.id;
    }
}
