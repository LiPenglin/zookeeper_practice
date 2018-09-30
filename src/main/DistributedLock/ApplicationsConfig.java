package DistributedLock;

class ApplicationsConfig {
    private String zooKeeperServersUrl;
    private int sessionTimeout;
    private String application;
    private String transaction;
    private String logPath;

    ApplicationsConfig(String zooKeeperServersUrl,
                       int sessionTimeout,
                       String application,
                       String transaction) {
        this.zooKeeperServersUrl = zooKeeperServersUrl;
        this.sessionTimeout = sessionTimeout;
        this.application = application;
        this.transaction = transaction;
    }

    String getZooKeeperServersUrl() {
        return zooKeeperServersUrl;
    }

    int getSessionTimeout() {
        return sessionTimeout;
    }

    String getTransaction() {
        return transaction;
    }

    public String getApplication() {
        return application;
    }

    public String getLogPath() {
        return logPath;
    }

    public void setLogPath(String logPath) {
        this.logPath = logPath;
    }
}
