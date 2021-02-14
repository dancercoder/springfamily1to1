package one.curator;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class GetStartMain {
    private static final Logger log = LoggerFactory.getLogger(GetStartMain.class);

    public static void main(String[] args) {
        CuratorFramework curator = CuratorFrameworkFactory
                .newClient("127.0.0.1:2181",
                        new ExponentialBackoffRetry(1000, 3));
        curator.start();

//        try {
//            curator.create().forPath("/curator/lock", "curator.lock".getBytes());
//        } catch (Exception e) {
//            log.error("error", e);
//        }

        try {
            InterProcessMutex lock = new InterProcessMutex(curator, "/curator/lock");
            if (lock.acquire(1000, TimeUnit.MILLISECONDS)) {
                try {
                    log.info("get the mutex lock");
                    try{
                        Thread.sleep(10000);
                    }catch(InterruptedException e){
                        log.error("error",e);
                    }
                } finally {
                    lock.release();
                }
            }
        } catch (Exception e) {
            log.error("error", e);
        }
    }
}
