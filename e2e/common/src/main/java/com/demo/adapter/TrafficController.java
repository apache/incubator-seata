package com.demo.adapter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;

/**
 * Perform tasks at regular intervals.
 */
@Slf4j
@Builder(toBuilder = true)
@AllArgsConstructor
@RequiredArgsConstructor
public class TrafficController {

    @Builder.Default
    private final int interval = 1000;
    @Builder.Default
    private final boolean logResult = true;
    private final Callable<?> sender;


    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(10, r -> {
        final Thread thread = new Thread(r, "traffic-controller");
        thread.setDaemon(true);
        return thread;
    });


    private ScheduledFuture<?> future;

    // Cycle task.
    public TrafficController start() {
        if (future == null) {
            future = executor.scheduleAtFixedRate(() -> {
                try {
                    final Object result = sender.call();
                    if (logResult) {
                        log.info("response: {}", result);
                    }
                } catch (final Exception e) {
                    log.error("failed to send request", e);
                }
            }, 0, interval, TimeUnit.MILLISECONDS);
        }

        return this;
    }

    public void stop() {
        if (future != null) {
            future.cancel(true);
            future = null;
        }
    }


}
