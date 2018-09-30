package DistributedLock;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import static org.apache.zookeeper.CreateMode.EPHEMERAL_SEQUENTIAL;
import static org.apache.zookeeper.Watcher.Event.EventType.NodeDeleted;
import static org.apache.zookeeper.Watcher.Event.KeeperState.SyncConnected;
import static org.apache.zookeeper.ZooDefs.Ids.OPEN_ACL_UNSAFE;

public class DistributedLock implements Watcher {
    private static final boolean WITHOUT_WATCH = false;
    private static final String LOCK_PREFIX = "_LOCK_";
    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private ZooKeeper session;
    private CountDownLatch synchronised;
    private String rootPath;
    private String transaction;
    private String currentChildPath;
    private String previousChild;
    private CountDownLatch executionLatch;

    private Path log;

    public DistributedLock(ApplicationsConfig config) {
        rootPath = String.format("/%s", config.getApplication());
        transaction = config.getTransaction();
        synchronised = new CountDownLatch(1);
        try {
            session = new ZooKeeper(
                    config.getZooKeeperServersUrl(),
                    config.getSessionTimeout(),
                    this
            );
            synchronised.await();
            if (session.exists(rootPath, WITHOUT_WATCH) == null) {
                session.create(rootPath, null, OPEN_ACL_UNSAFE, EPHEMERAL_SEQUENTIAL);
            }
            // check log path is null.
            log = Paths.get(config.getLogPath());
            Files.delete(log);
        } catch (Exception ignored) {
        }

    }

    @Override
    public void process(WatchedEvent event) {
        if (synchronised.getCount() == 1
                && event.getState().equals(SyncConnected)) {
            synchronised.countDown();
            return;
        }

        if (executionLatch.getCount() == 1
                && event.getState().equals(SyncConnected)
                && event.getType().equals(NodeDeleted)) {
            String logMessage = previousChild + " deleted.";
            printLog(logMessage);
            executionLatch.countDown();
        }
    }

    private boolean tryLock() {

        try {
            currentChildPath = session.create(getAbsolutePath(transaction + LOCK_PREFIX), null, OPEN_ACL_UNSAFE, EPHEMERAL_SEQUENTIAL);
            String currentChild = currentChildPath.substring(rootPath.length() + 1);
            printLog(currentChild + " created.");

            List<String> children = getChildren();
            printLog(children.get(0) + " -- " + currentChild);
            if (children.get(0).equals(currentChild)) {
                return true;
            }

            previousChild = children.get(Collections.binarySearch(children, currentChild) - 1);
            printLog(getAbsolutePath(previousChild) + " register watcher.");
            registerWatcherAtPreviousChild();
        } catch (Exception ignored) {
        }
        return false;
    }

    private void unlock() {
        try {
            session.delete(currentChildPath, -1);
            currentChildPath = null;
        } catch (Exception ignored) {
        }
    }

    /**
     * If the previous child is deleted before register watcher then remember to
     * check the previous child is alive async.
     *
     * @return the stat of the previous child.
     */
    private Stat registerWatcherAtPreviousChild() {
        try {
            return session.exists(getAbsolutePath(previousChild), this);
        } catch (Exception ignored) {
        }
        return null;
    }

    private void checkPreviousChildIsAliveAsync() {
        new Thread(() -> {
            while (registerWatcherAtPreviousChild() != null) {
                printLog("check " + previousChild + " is alive --- ...await...");
            }
            executionLatch.countDown();
        }).start();
    }

    private String getAbsolutePath(String child) {
        return String.format("%s/%s", rootPath, child);
    }

    private List<String> getChildren() throws Exception {
        return session.getChildren(rootPath, WITHOUT_WATCH)
                .stream()
                .filter(child -> child.split(LOCK_PREFIX)[0].equals(transaction))
                .sorted()
                .collect(Collectors.toList());
    }

    private void printLog(String logMessage) {
//        System.out.println(logMessage);
        try {
            Files.write(
                    log,
                    (dateFormat.format(System.currentTimeMillis()) + " : " + logMessage + "\n").getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
        } catch (IOException ignored) {
        }
    }

    public void doSomething(Object something) {
        printLog("do " + something + ".");
        unlock();
    }

    public boolean lock() {
        try {
            if (tryLock()) {
                String logMessage = "get lock.";
                printLog(logMessage);
                return true;
            } else {
                executionLatch = new CountDownLatch(1);
                checkPreviousChildIsAliveAsync();
                executionLatch.await();
            }
        } catch (Exception ignored) {
        }
        return true;
    }
}
