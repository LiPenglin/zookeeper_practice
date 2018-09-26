package DistributedLock;

import junit.framework.Assert;
import org.apache.zookeeper.ZooKeeper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ExclusiveLockFacts {

    private static ApplicationsConfig config;

    @BeforeAll
    static void initializeExclusiveLock() {
        String servers = "192.168.1.10:2181,192.168.1.20:2181,192.168.1.30:2181";
        int timeout = 20000;
        config = new ApplicationsConfig(servers, timeout);
    }

    @Test
    void session_state_should_be_connected_when_receive_sync_connected_type_event() throws Exception{
        ExclusiveLock lock = new ExclusiveLock(config);
        Assert.assertEquals(ZooKeeper.States.CONNECTED , lock.getSessionState());
    }
}
