package io.seata.server.coordinator;

import java.util.Collection;

import io.seata.core.context.RootContext;
import io.seata.core.exception.TransactionException;
import io.seata.server.session.BranchSession;
import io.seata.server.session.GlobalSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * The Session foreach utils
 *
 * @author wang.liang
 */
public class SessionForeachUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(SessionForeachUtils.class);

    public static final Boolean CONTINUE = null;

    /**
     * Foreach global sessions.
     *
     * @param sessions the global sessions
     * @param handler  the handler
     */
    public static void foreach(Collection<GlobalSession> sessions, GlobalSessionHandler handler) {
        for (GlobalSession globalSession : sessions) {
            try {
                MDC.put(RootContext.MDC_KEY_XID, globalSession.getXid());
                handler.handle(globalSession);
            } catch (Exception e) {
                LOGGER.error("handle global session failed: {}", globalSession.getXid());
            } finally {
                MDC.remove(RootContext.MDC_KEY_XID);
            }
        }
    }

    /**
     * Foreach branch sessions.
     *
     * @param sessions the branch session
     * @param handler  the handler
     */
    public static Boolean foreach(Collection<BranchSession> sessions, BranchSessionHandler handler) throws TransactionException {
        Boolean result;
        for (BranchSession branchSession : sessions) {
            try {
                MDC.put(RootContext.MDC_KEY_BRANCH_ID, String.valueOf(branchSession.getBranchId()));
                result = handler.handle(branchSession);
                if (result == null) {
                    continue;
                }
                return result;
            } finally {
                MDC.remove(RootContext.MDC_KEY_XID);
            }
        }
        return null;
    }
}
