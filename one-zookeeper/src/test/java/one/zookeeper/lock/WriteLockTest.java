package one.zookeeper.lock;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.common.Time;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.AfterEach;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class WriteLockTest {

    protected int sessionTimeout = 10 * 1000;
    protected String dir = "/" + getClass().getName();
    protected WriteLock[] nodes;
    protected CountDownLatch latch = new CountDownLatch(1);
    private boolean restartServer = false;
    private boolean workAroundClosingLastZNodeFails = true;
    private boolean killLeader = true;

    @Test
    public void testRun() throws Exception {
        runTest(3);
    }

    class LockCallback implements LockListener {

        public void lockAcquired() {
            latch.countDown();
        }

        public void lockReleased() {

        }

    }

    protected void runTest(int count) throws Exception {
        nodes = new WriteLock[count];
        for (int i = 0; i < count; i++) {
            ZooKeeper keeper = createClient();
            WriteLock leader = new WriteLock(keeper, dir, null);
            leader.setLockListener(new LockCallback());
            nodes[i] = leader;

            leader.lock();
        }

        // lets wait for any previous leaders to die and one of our new
        // nodes to become the new leader
        latch.await(30, TimeUnit.SECONDS);

        WriteLock first = nodes[0];
        dumpNodes(count);

        // lets assert that the first election is the leader
        assertTrue("The first znode should be the leader " + first.getId(), first.isOwner());

        for (int i = 1; i < count; i++) {
            WriteLock node = nodes[i];
            assertFalse("Node should not be the leader " + node.getId(), node.isOwner());
        }

        if (count > 1) {
            if (killLeader) {
                System.out.println("Now killing the leader");
                // now lets kill the leader
                latch = new CountDownLatch(1);
                first.unlock();
                latch.await(30, TimeUnit.SECONDS);
                //Thread.sleep(10000);
                WriteLock second = nodes[1];
                dumpNodes(count);
                // lets assert that the first election is the leader
                assertTrue("The second znode should be the leader " + second.getId(), second.isOwner());

                for (int i = 2; i < count; i++) {
                    WriteLock node = nodes[i];
                    assertFalse("Node should not be the leader " + node.getId(), node.isOwner());
                }
            }

            if (restartServer) {
                // now lets stop the server
                System.out.println("Now stopping the server");
//                stopServer();
                Thread.sleep(10000);

                // TODO lets assert that we are no longer the leader
                dumpNodes(count);

                System.out.println("Starting the server");
//                startServer();
                Thread.sleep(10000);

                for (int i = 0; i < count - 1; i++) {
                    System.out.println("Calling acquire for node: " + i);
                    nodes[i].lock();
                }
                dumpNodes(count);
                System.out.println("Now closing down...");
            }
        }
    }

    protected void dumpNodes(int count) {
        for (int i = 0; i < count; i++) {
            WriteLock node = nodes[i];
            System.out.println("node: " + i + " id: " + node.getId() + " is leader: " + node.isOwner());
        }
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (nodes != null) {
            for (int i = 0; i < nodes.length; i++) {
                WriteLock node = nodes[i];
                if (node != null) {
                    System.out.println("Closing node: " + i);
                    node.close();
                    if (workAroundClosingLastZNodeFails && i == nodes.length - 1) {
                        System.out.println("Not closing zookeeper: " + i + " due to bug!");
                    } else {
                        System.out.println("Closing zookeeper: " + i);
                        node.getZookeeper().close();
                        System.out.println("Closed zookeeper: " + i);
                    }
                }
            }
        }
        System.out.println("Now lets stop the server");
        tearDown();

    }

    private ZooKeeper createClient() throws Exception, InterruptedException {
        return createClient("127.0.0.1:2181");
    }

    private ZooKeeper createClient(String hp)
            throws Exception, InterruptedException {
        String cxnString = "127.0.0.1:2181";
        CountdownWatcher watcher = new CountdownWatcher();
        ZooKeeper zk = new ZooKeeper(cxnString, sessionTimeout, watcher);
        try {
            watcher.waitForConnected(30000);
        } catch (InterruptedException | TimeoutException e) {
            Assert.fail("ZooKeeper client can not connect to " + cxnString);
        }
        return zk;
    }

    public static class CountdownWatcher implements Watcher {
        // XXX this doesn't need to be volatile! (Should probably be final)
        volatile CountDownLatch clientConnected;
        // Set to true when connected to a read-only server, or a read-write (quorum) server.
        volatile boolean connected;
        // Set to true when connected to a quorum server.
        volatile boolean syncConnected;
        // Set to true when connected to a quorum server in read-only mode
        volatile boolean readOnlyConnected;

        public CountdownWatcher() {
            reset();
        }

        synchronized public void reset() {
            clientConnected = new CountDownLatch(1);
            connected = false;
            syncConnected = false;
            readOnlyConnected = false;
        }

        synchronized public void process(WatchedEvent event) {
            Event.KeeperState state = event.getState();
            if (state == Event.KeeperState.SyncConnected) {
                connected = true;
                syncConnected = true;
                readOnlyConnected = false;
            } else if (state == Event.KeeperState.ConnectedReadOnly) {
                connected = true;
                syncConnected = false;
                readOnlyConnected = true;
            } else {
                connected = false;
                syncConnected = false;
                readOnlyConnected = false;
            }

            notifyAll();
            if (connected) {
                clientConnected.countDown();
            }
        }

        synchronized public boolean isConnected() {
            return connected;
        }

        synchronized public void waitForConnected(long timeout)
                throws InterruptedException, TimeoutException {
            long expire = Time.currentElapsedTime() + timeout;
            long left = timeout;
            while (!connected && left > 0) {
                wait(left);
                left = expire - Time.currentElapsedTime();
            }
            if (!connected) {
                throw new TimeoutException("Failed to connect to ZooKeeper server.");

            }
        }

        synchronized public void waitForSyncConnected(long timeout)
                throws InterruptedException, TimeoutException {
            long expire = Time.currentElapsedTime() + timeout;
            long left = timeout;
            while (!syncConnected && left > 0) {
                wait(left);
                left = expire - Time.currentElapsedTime();
            }
            if (!syncConnected) {
                throw new TimeoutException("Failed to connect to read-write ZooKeeper server.");
            }
        }

        synchronized public void waitForReadOnlyConnected(long timeout)
                throws InterruptedException, TimeoutException {
            long expire = System.currentTimeMillis() + timeout;
            long left = timeout;
            while (!readOnlyConnected && left > 0) {
                wait(left);
                left = expire - System.currentTimeMillis();
            }
            if (!readOnlyConnected) {
                throw new TimeoutException("Failed to connect in read-only mode to ZooKeeper server.");
            }
        }

        synchronized public void waitForDisconnected(long timeout)
                throws InterruptedException, TimeoutException {
            long expire = Time.currentElapsedTime() + timeout;
            long left = timeout;
            while (connected && left > 0) {
                wait(left);
                left = expire - Time.currentElapsedTime();
            }
            if (connected) {
                throw new TimeoutException("Did not disconnect");

            }
        }
    }
}
