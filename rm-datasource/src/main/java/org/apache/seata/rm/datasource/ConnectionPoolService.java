package org.apache.seata.rm.datasource;

import org.apache.seata.rm.datasource.entity.ConnectionPoolMetrics;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ConnectionPoolService implements Runnable {

    private DruidConnectionPoolManager manager = DruidConnectionPoolManager.getInstance();

    private ConnectionPoolMetricsSender sender = ConnectionPoolMetricsSender.getInstance();

    public void init() throws Exception {
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r);
            thread.setDaemon(true);
            return thread;
        }).scheduleAtFixedRate(this, 0, 1, TimeUnit.SECONDS);
    }


    @Override
    public void run() {
        List<ConnectionPoolMetrics> metricsList = manager.getConnectionPoolMetricsList();

        metricsList.forEach(metrics -> sender.offer(metrics));
    }
}
