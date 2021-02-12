package one.curator;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LockingMain {

    private static final int QTY = 5;
    private static final int REPETIRIONS = QTY * 10;

    private static final String PATH = "one-curator/locks";

    public static void main(String[] args) {
        final MockLimitedResource resource = new MockLimitedResource();
        ExecutorService service = Executors.newFixedThreadPool(QTY);

        try {

            for (int i = 0; i < QTY; i++) {
                final int index = i;
                Callable<Void> task = new Callable<Void>() {
                    public Void call() throws Exception {
                        CuratorFramework client = CuratorFrameworkFactory
                                .newClient("192.168.1.1:2181"
                                        , new ExponentialBackoffRetry(1000, 3));
                        try{
                            client.start();
                            ExampleC
                        }
                    }
                }
            }
        }

    }
}


























