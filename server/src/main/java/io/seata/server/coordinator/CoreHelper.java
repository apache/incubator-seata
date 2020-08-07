package io.seata.server.coordinator;

import io.seata.server.session.BranchSession;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author wang.liang
 */
public class CoreHelper {
    private CoreHelper() {
    }

    private static final ThreadLocal<Boolean> ALLOW_END_COMMITTED = new ThreadLocal<>();

    /**
     * Is allow end committed.
     *
     * @return the allow end committed
     */
    static boolean isAllowEndCommitted() {
        return Boolean.TRUE.equals(ALLOW_END_COMMITTED.get());
    }

    /**
     * Take out branch sessions that can be committed async.
     *
     * @param branchSessions the branch sessions
     * @return the branch sessions that can be committed async
     */
    static ArrayList<BranchSession> takeOutBranchSessionsCanBeCommittedAsync(List<BranchSession> branchSessions) {
        ArrayList<BranchSession> branchSessionsCanBeCommittedAsync = new ArrayList<>();

        BranchSession branchSession;
        Iterator<BranchSession> iter = branchSessions.iterator();
        while (iter.hasNext()) {
            branchSession = iter.next();
            if (branchSession.canBeCommittedAsync()) {
                iter.remove();
                branchSessionsCanBeCommittedAsync.add(branchSession);
            }
        }

        if (branchSessionsCanBeCommittedAsync.size() > 0) {
            ALLOW_END_COMMITTED.set(false);
        }

        return branchSessionsCanBeCommittedAsync;
    }

    /**
     * Put back branch sessions that can be committed async.
     *
     * @param branchSessions                    the branch sessions
     * @param branchSessionsCanBeCommittedAsync the branch sessions that can be committed async
     */
    static void putBackBranchSessionsCanBeCommittedAsync(List<BranchSession> branchSessions, List<BranchSession> branchSessionsCanBeCommittedAsync) {
        branchSessions.addAll(branchSessionsCanBeCommittedAsync);
        ALLOW_END_COMMITTED.remove();
    }
}
