package io.seata.tm.api.transaction;

/**
 * Used to avoid early hook execution when transactions are nested
 * @author ninggc
 */
public class TransactionDepthManager {
    private static final ThreadLocal<Integer> depth = ThreadLocal.withInitial(() -> 0);

    public static void depthInc() {
        depth.set(depth.get() + 1);
    }

    public static void depthDec() {
        depth.set(depth.get() - 1);
    }

    public static boolean isOriginDepth() {
        return Integer.valueOf(0).equals(depth.get());
    }

    public static Integer suspend() {
        depth.remove();
        return depth.get();
    }

    public static void resume(Integer resumeDepth) {
        depth.set(resumeDepth);
    }
}
