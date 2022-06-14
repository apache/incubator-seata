package io.seata.server.logging.logback.extend;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.util.Duration;
import net.logstash.logback.appender.destination.DestinationConnectionStrategy;
import net.logstash.logback.appender.destination.PreferPrimaryDestinationConnectionStrategy;
import net.logstash.logback.appender.destination.RandomDestinationConnectionStrategy;
import net.logstash.logback.appender.destination.RoundRobinDestinationConnectionStrategy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author wlx
 * @date 2022/6/14 10:56 下午
 */
public class DestinationConnectionStrategyCreatorTest {

    @Test
    public void destinationConnectionStrategyCreatorTest() {
        // preferPrimary random roundRobin
        DestinationConnectionStrategy preferPrimary =
                SeataLogbackLoggingLogstashExtendAppender.DestinationConnectionStrategyCreator.createDestinationConnectionStrategy("preferPrimary");
        DestinationConnectionStrategy random = SeataLogbackLoggingLogstashExtendAppender.DestinationConnectionStrategyCreator.createDestinationConnectionStrategy("random");
        DestinationConnectionStrategy roundRobin = SeataLogbackLoggingLogstashExtendAppender.DestinationConnectionStrategyCreator.createDestinationConnectionStrategy("roundRobin");

        assertThat(preferPrimary instanceof PreferPrimaryDestinationConnectionStrategy).isTrue();
        assertThat(random instanceof RandomDestinationConnectionStrategy).isTrue();
        assertThat(roundRobin instanceof RoundRobinDestinationConnectionStrategy).isTrue();
    }


    @Test
    public void destinationConnectionStrategyWithTtlCreatorTest() {
        // preferPrimary random roundRobin
        DestinationConnectionStrategy preferPrimary =
                SeataLogbackLoggingLogstashExtendAppender.DestinationConnectionStrategyCreator.createDestinationConnectionStrategy("preferPrimary", "1000");
        DestinationConnectionStrategy random = SeataLogbackLoggingLogstashExtendAppender.DestinationConnectionStrategyCreator.createDestinationConnectionStrategy("random", "1000");
        DestinationConnectionStrategy roundRobin = SeataLogbackLoggingLogstashExtendAppender.DestinationConnectionStrategyCreator.createDestinationConnectionStrategy("roundRobin", "1000");

        assertThat(preferPrimary instanceof PreferPrimaryDestinationConnectionStrategy).isTrue();
        assertThat(random instanceof RandomDestinationConnectionStrategy).isTrue();
        assertThat(roundRobin instanceof RoundRobinDestinationConnectionStrategy).isTrue();

        PreferPrimaryDestinationConnectionStrategy preferPrimaryDestinationConnectionStrategy = (PreferPrimaryDestinationConnectionStrategy) preferPrimary;
        Duration secondaryConnectionTTL = preferPrimaryDestinationConnectionStrategy.getSecondaryConnectionTTL();
        assertThat(secondaryConnectionTTL.getMilliseconds() == 1000);

        RandomDestinationConnectionStrategy randomDestinationConnectionStrategy = (RandomDestinationConnectionStrategy) random;
        Duration randomDestinationConnectionStrategyConnectionTTL = randomDestinationConnectionStrategy.getConnectionTTL();
        assertThat(randomDestinationConnectionStrategyConnectionTTL.getMilliseconds() == 1000);

        RoundRobinDestinationConnectionStrategy roundRobinDestinationConnectionStrategy = (RoundRobinDestinationConnectionStrategy) roundRobin;
        Duration roundRobinDestinationConnectionStrategyConnectionTTL = roundRobinDestinationConnectionStrategy.getConnectionTTL();
        assertThat(roundRobinDestinationConnectionStrategyConnectionTTL.getMilliseconds() == 1000);
    }

    @Test
    public void destinationConnectionStrategyCreatorThrowTest(){
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            SeataLogbackLoggingLogstashExtendAppender.DestinationConnectionStrategyCreator.createDestinationConnectionStrategy("1234", "1000");
        });
    }


}
