package org.apache.seata.rm.datasource;

import org.apache.seata.rm.datasource.entity.ConnectionPoolMetrics;

import java.util.concurrent.LinkedBlockingQueue;

public class ConnectionPoolMetricsSender {

    private final static LinkedBlockingQueue<ConnectionPoolMetrics> queue = new LinkedBlockingQueue<>(600);

    private static ConnectionPoolMetricsSender INSTANCE;

    public static ConnectionPoolMetricsSender getInstance() {
        if (INSTANCE == null) {
            synchronized (ConnectionPoolMetricsSender.class) {
                if (INSTANCE == null) {
                    INSTANCE = new ConnectionPoolMetricsSender();
                }
            }
        }
        return INSTANCE;
    }

    private ConnectionPoolMetricsSender() {

    }

    public void offer(ConnectionPoolMetrics metrics) {
        if (!queue.offer(metrics)) {
            queue.poll();
            queue.offer(metrics);
        }
    }

}
