package io.seata.server.logging.logback.appender;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import ch.qos.logback.classic.spi.ILoggingEvent;
import net.logstash.logback.LogstashFormatter;
import net.logstash.logback.composite.CompositeJsonFormatter;
import net.logstash.logback.composite.JsonProvider;
import net.logstash.logback.composite.JsonProviders;
import net.logstash.logback.composite.LogstashVersionJsonProvider;
import net.logstash.logback.encoder.LogstashEncoder;

/**
 * @author wang.liang
 */
public class OverrideLogstashEncoder extends LogstashEncoder {

    @Override
    protected CompositeJsonFormatter<ILoggingEvent> createFormatter() {
        LogstashFormatter formatter = new LogstashFormatter(this);

        Set<Class<?>> excludeProviderClasses = new HashSet<>();
        // the `@version` provider
        excludeProviderClasses.add(LogstashVersionJsonProvider.class);

        // do exclude Providers
        this.doExcludeProviders(formatter, excludeProviderClasses);

        return formatter;
    }

    /**
     * Exclude Providers
     *
     * @param formatter              the formatter
     * @param excludeProviderClasses the exclude provider classes
     */
    private void doExcludeProviders(LogstashFormatter formatter, Set<Class<?>> excludeProviderClasses) {
        JsonProviders<?> providers = formatter.getProviders();
        for (JsonProvider<?> provider : new ArrayList<>(providers.getProviders())) {
            if (excludeProviderClasses.contains(provider.getClass())) {
                providers.removeProvider((JsonProvider) provider);
            }
        }
    }
}
