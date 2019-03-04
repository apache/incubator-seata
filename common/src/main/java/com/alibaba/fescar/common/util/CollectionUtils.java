package com.alibaba.fescar.common.util;

import java.util.Collection;

/**
 * @author zhangsen
 */
public class CollectionUtils {


    public static boolean isEmpty(Collection col){
        return !isNotEmpty(col);
    }

    public static boolean isNotEmpty(Collection col){
        return col != null && col.size() > 0;
    }
}
