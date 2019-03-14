package com.alibaba.fescar.common.util;

import java.util.Collection;

/**
 * The type Collection utils.
 */
public class CollectionUtils {
    /**
     * Is not empty boolean.
     *
     * @param coll the coll
     * @return the boolean
     */
    public static boolean isNotEmpty(Collection<?> coll) {
        return null != coll && !coll.isEmpty();
    }
    
    /**
     * Is empty boolean.
     *
     * @param coll the coll
     * @return the boolean
     */
    public static boolean isEmpty(Collection<?> collection) {
        return null == collection || collection.isEmpty();
    }
}
