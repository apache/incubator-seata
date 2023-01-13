package io.seata.tm.api;

/**
 * @author leezongjie
 * @date 2023/1/13
 */
public class FailureHandlerHolder {

    private static FailureHandler FAILURE_HANDLER_HOLDER = new DefaultFailureHandlerImpl();

    public static void setFailureHandler(FailureHandler failureHandler) {
        if (failureHandler != null) {
            FAILURE_HANDLER_HOLDER = failureHandler;
        }
    }

    public static FailureHandler getFailureHandler() {
        return FAILURE_HANDLER_HOLDER;
    }

}
