package io.seata.server.session;

import io.seata.core.model.GlobalStatus;

/**
 * The type change status validator.
 *
 * @author Bughue
 */
public class ChangeStatusValidator {

    /**
     * is timeout global status timeout
     *
     * @param status the global session
     */
    public static boolean isTimeoutGlobalStatus(GlobalStatus status) {
        return status == GlobalStatus.TimeoutRollbacked
                || status == GlobalStatus.TimeoutRollbackFailed
                || status == GlobalStatus.TimeoutRollbacking
                || status == GlobalStatus.TimeoutRollbackRetrying;
    }

    /**
     * is rollback global status timeout
     *
     * @param status the global session
     */
    public static boolean isRollbackGlobalStatus(GlobalStatus status) {
        return status == GlobalStatus.Rollbacking
                || status == GlobalStatus.RollbackRetrying
                || status == GlobalStatus.Rollbacked
                || status == GlobalStatus.RollbackFailed
                || status == GlobalStatus.RollbackRetryTimeout;
    }

    /**
     * is commit global status timeout
     *
     * @param status the global session
     */
    public static boolean isCommitGlobalStatus(GlobalStatus status) {
        return status == GlobalStatus.Committing
                || status == GlobalStatus.AsyncCommitting
                || status == GlobalStatus.CommitRetrying
                || status == GlobalStatus.Committed
                || status == GlobalStatus.CommitFailed
                || status == GlobalStatus.CommitRetryTimeout;
    }

    /**
     * check the relation of before status and after status
     *
     * @param before the global session
     * @param after the global session
     */
    public static boolean validateUpdateStatus(GlobalStatus before, GlobalStatus after) {
        if (isTimeoutGlobalStatus(before) && isCommitGlobalStatus(after)) {
            return false;
        }
        if (isCommitGlobalStatus(before) && isTimeoutGlobalStatus(after)) {
            return false;
        }
        if (isRollbackGlobalStatus(before) && isCommitGlobalStatus(after)) {
            return false;
        }
        if (isCommitGlobalStatus(before) && isRollbackGlobalStatus(after)) {
            return false;
        }
        return true;
    }
}
