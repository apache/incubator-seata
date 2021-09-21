package seata.e2e.helper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

/**
 * @author xjl
 * @Description: An asynchronous scheduled task executor
 */
public class CronTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(CronTask.class);

    private int interval;
    private Callable<?> sender;
    private int count = 0;
    private TimeCountHelper timeCountHelper = new TimeCountHelper();
    private ScheduledFuture<?> future;

    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors(),
        r -> {
        final Thread thread = new Thread(r, "CronTask");
        thread.setDaemon(true);
        return thread;});

    public CronTask(int interval, Callable<?> sender) {
        this.interval = interval;
        this.sender = sender;
    }

    /**
     * Start scheduled task
     */
    public void start() {
        timeCountHelper.startTimeCount();
        if (future == null) {
            future = executor.scheduleAtFixedRate(() -> {
                try {
                    final Object result = sender.call();
                    LOGGER.info("response: {}", result);
                    count++;
                } catch (final Exception e) {
                    LOGGER.error("failed to send request", e);
                }
            }, 0, interval, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Stop scheduled task
     */
    public void stop() {
        if (future != null) {
            future.cancel(true);
            future = null;
            long time = timeCountHelper.stopTimeCount();
            LOGGER.info("Task cost time: {} s", time / 1000);
            LOGGER.info("Task completed, total {}", count);
            count = 0;
        }
    }


}
