package io.seata.core.context;

import io.seata.core.model.GlobalLockConfig;

/** use this class to access current GlobalLockConfig from anywhere
 * @author selfishlover
 */
public class GlobalLockConfigHolder {

    private static ThreadLocal<GlobalLockConfig> holder = new ThreadLocal<>();

    public static GlobalLockConfig getCurrentGlobalLockConfig() {
        return holder.get();
    }

    public static GlobalLockConfig setAndReturnPrevious(GlobalLockConfig config) {
        GlobalLockConfig previous = holder.get();
        holder.set(config);
        return previous;
    }

    public static void remove() {
        holder.remove();
    }
}
