package DistributedLock;

class ApplicationsConfig {
    private String zooKeeperServersUrl;
    private int sessionTimeout;

    ApplicationsConfig(String zooKeeperServersUrl, int sessionTimeout) {
        this.zooKeeperServersUrl = zooKeeperServersUrl;
        this.sessionTimeout = sessionTimeout;
    }

    String getZooKeeperServersUrl() {
        return zooKeeperServersUrl;
    }

    int getSessionTimeout() { return sessionTimeout; }
}
