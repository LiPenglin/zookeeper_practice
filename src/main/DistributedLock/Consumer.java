package DistributedLock;

public class Consumer {
    public static void main(String []args) {
        String servers = "192.168.1.10:2181,192.168.1.20:2181,192.168.1.30:2181";
        int timeout = 20000;
        String application = "PARENT";
        String transaction = "CHILD";
        String logPrefix = "e://log//";
        String logSuffix = ".txt";


        for (int n = 0; n < 10; n++) {
            int finalN = n;
            new Thread(() -> {
                ApplicationsConfig config = new ApplicationsConfig(servers, timeout, application, transaction);
                config.setLogPath(logPrefix+finalN+logSuffix);
                DistributedLock distributedLock = new DistributedLock(config);
                for (int i = 0; i < 100; i++) {
                    if (distributedLock.lock()) {
                        distributedLock.doSomething(finalN);
                    }
                    try {
                        Thread.sleep((long) (Math.random()*100));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }
}
