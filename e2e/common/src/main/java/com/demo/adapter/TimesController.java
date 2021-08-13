package com.demo.adapter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Perform tasks at fixed number of times.
 */
@Slf4j
@Builder(toBuilder = true)
@AllArgsConstructor
public class TimesController {

    @Builder.Default
    private final int interval = 1000;
    @Builder.Default
    private final boolean logResult = true;
    private final Callable<?> sender;

    private static int times;

    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(10, r -> {
        final Thread thread = new Thread(r, "times-controller");
        thread.setDaemon(true);
        return thread;
    });


    public TimesController start(int times) {
        TimesController.times = times;
        executor.scheduleAtFixedRate(() -> {
            try {
                if (TimesController.times > 0) {
                    final Object result = sender.call();
                    if (logResult) {
                        log.info("response: {}", result);
                    }
                    TimesController.times--;
                } else {
                    List<Runnable> runnables = executor.shutdownNow();
                    if (runnables.size() != 0) {
                        log.error("There are some requests not work, the num is ", runnables.size());
                    }
                }
            } catch (final Exception e) {
                log.error("failed to send request", e);
            }
        }, 0, interval, TimeUnit.MILLISECONDS);


        return this;
    }
}
