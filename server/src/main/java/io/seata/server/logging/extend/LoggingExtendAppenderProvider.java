package io.seata.server.logging.extend;

/**
 * The type of LoggingExtendAppenderProvider to append specific appender
 * to logging system.
 *
 * @author wlx
 * @date 2022/5/30 11:23 下午
 */
public interface LoggingExtendAppenderProvider {

    /**
     * append the logging appender whit spring environment to logging system.
     */
    void appendTo();

    /**
     * should append the logging appender to logging system.
     *
     * @return false default
     */
    default boolean shouldAppend() {
        return false;
    }

}
