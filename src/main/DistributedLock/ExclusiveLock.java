package DistributedLock;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooKeeper;

import java.util.concurrent.CountDownLatch;

class ExclusiveLock {
    private ZooKeeper sessionInstance;
    private static CountDownLatch latch = new CountDownLatch(1);

    ExclusiveLock(ApplicationsConfig appConfig) throws Exception{
        sessionInstance = new ZooKeeper(
                appConfig.getZooKeeperServersUrl(),
                appConfig.getSessionTimeout(),
                ExclusiveLock::establishConnection);
        latch.await();
    }

   Enum getSessionState() {
        return this.sessionInstance.getState();
   }

    private static void establishConnection(WatchedEvent establishEvent) {
        if (latch.getCount() > 0 && establishEvent.getState().equals(KeeperState.SyncConnected)) {
            latch.countDown();
        }
    }
}
