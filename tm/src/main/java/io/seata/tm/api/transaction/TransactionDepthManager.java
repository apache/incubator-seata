package io.seata.tm.api.transaction;

/**
 * Used to avoid early hook execution when transactions are nested
 * @author ninggc
 */
public class TransactionDepthManager {
    private static final ThreadLocal<Integer> LOCAL_DEPTH = ThreadLocal.withInitial(() -> 0);

    public static void depthInc() {
        LOCAL_DEPTH.set(LOCAL_DEPTH.get() + 1);
    }

    public static void depthDec() {
        LOCAL_DEPTH.set(LOCAL_DEPTH.get() - 1);
    }

    public static boolean isOriginDepth() {
        // Compatible with last depthDec action
        return LOCAL_DEPTH.get() <= 1;
    }

    public static Integer suspend() {
        LOCAL_DEPTH.remove();
        return LOCAL_DEPTH.get();
    }

    public static void resume(Integer resumeDepth) {
        LOCAL_DEPTH.set(resumeDepth);
    }

    public static void clear() {
        LOCAL_DEPTH.remove();
    }
}
