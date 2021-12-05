/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.spring.util;

import org.springframework.aop.Advisor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.OrderUtils;

/**
 * The type Order util.
 *
 * @author wang.liang
 */
public class OrderUtil {

    private OrderUtil() {
    }

    /**
     * Return the order on the object.
     *
     * @param obj the obj
     * @return the order
     */
    public static int getOrder(Object obj) {
        if (obj instanceof Ordered) {
            return ((Ordered) obj).getOrder();
        } else {
            Integer order = OrderUtils.getOrder(obj.getClass());
            return order == null ? Ordered.LOWEST_PRECEDENCE : order;
        }
    }

    /**
     * Is lower than.
     *
     * @param source the source
     * @param target the target
     * @return the boolean
     */
    public static boolean lowerThan(Integer source, Integer target) {
        if (source == null) {
            source = Ordered.LOWEST_PRECEDENCE;
        }
        if (target == null) {
            target = Ordered.LOWEST_PRECEDENCE;
        }
        return source > target;
    }

    /**
     * Is higher than.
     *
     * @param source the source
     * @param target the target
     * @return the boolean
     */
    public static boolean higherThan(Integer source, Integer target) {
        if (source == null) {
            source = Ordered.LOWEST_PRECEDENCE;
        }
        if (target == null) {
            target = Ordered.LOWEST_PRECEDENCE;
        }
        return source < target;
    }

    /**
     * Is lower or equals.
     *
     * @param source the source
     * @param target the target
     * @return the boolean
     */
    public static boolean lowerOrEquals(Integer source, Integer target) {
        if (source == null) {
            source = Ordered.LOWEST_PRECEDENCE;
        }
        if (target == null) {
            target = Ordered.LOWEST_PRECEDENCE;
        }
        return source >= target;
    }

    /**
     * Is higher or equals.
     *
     * @param source the source
     * @param target the target
     * @return the boolean
     */
    public static boolean higherOrEquals(Integer source, Integer target) {
        if (source == null) {
            source = Ordered.LOWEST_PRECEDENCE;
        }
        if (target == null) {
            target = Ordered.LOWEST_PRECEDENCE;
        }
        return source <= target;
    }

    /**
     * Is lower than.
     *
     * @param source the source
     * @param target the target
     * @return the boolean
     */
    public static boolean lowerThan(Class<?> source, Class<?> target) {
        return source.getSimpleName().compareTo(target.getSimpleName()) > 0;
    }

    /**
     * Is higher than.
     *
     * @param source the source
     * @param target the target
     * @return the boolean
     */
    public static boolean higherThan(Class<?> source, Class<?> target) {
        return source.getSimpleName().compareTo(target.getSimpleName()) < 0;
    }

    /**
     * Is lower or equals.
     *
     * @param source the source
     * @param target the target
     * @return the boolean
     */
    public static boolean lowerOrEquals(Class<?> source, Class<?> target) {
        return source.getSimpleName().compareTo(target.getSimpleName()) >= 0;
    }

    /**
     * Is higher or equals.
     *
     * @param source the source
     * @param target the target
     * @return the boolean
     */
    public static boolean higherOrEquals(Class<?> source, Class<?> target) {
        return source.getSimpleName().compareTo(target.getSimpleName()) <= 0;
    }

    /**
     * Is lower than.
     *
     * @param source the source
     * @param target the target
     * @return the boolean
     */
    public static boolean lowerThan(Advisor source, Advisor target) {
        int sourceOrder = getOrder(source);
        int targetOrder = getOrder(target);

        if (lowerThan(sourceOrder, targetOrder)) {
            return true;
        } else {
            return sourceOrder == targetOrder && lowerThan(source.getAdvice().getClass(), target.getAdvice().getClass());
        }
    }

    /**
     * Is higher or equals.
     *
     * @param source the source
     * @param target the target
     * @return the boolean
     */
    public static boolean higherThan(Advisor source, Advisor target) {
        int sourceOrder = getOrder(source);
        int targetOrder = getOrder(target);

        if (higherThan(sourceOrder, targetOrder)) {
            return true;
        } else {
            return sourceOrder == targetOrder && higherThan(source.getAdvice().getClass(), target.getAdvice().getClass());
        }
    }

    /**
     * Is lower or equals.
     *
     * @param source the source
     * @param target the target
     * @return the boolean
     */
    public static boolean lowerOrEquals(Advisor source, Advisor target) {
        int sourceOrder = getOrder(source);
        int targetOrder = getOrder(target);

        if (lowerThan(sourceOrder, targetOrder)) {
            return true;
        } else {
            return sourceOrder == targetOrder && lowerOrEquals(source.getAdvice().getClass(), target.getAdvice().getClass());
        }
    }

    /**
     * Is higher or equals.
     *
     * @param source the source
     * @param target the target
     * @return the boolean
     */
    public static boolean higherOrEquals(Advisor source, Advisor target) {
        int sourceOrder = getOrder(source);
        int targetOrder = getOrder(target);

        if (higherThan(sourceOrder, targetOrder)) {
            return true;
        } else {
            return sourceOrder == targetOrder && higherOrEquals(source.getAdvice().getClass(), target.getAdvice().getClass());
        }
    }

    /**
     * Lower.
     *
     * @param orderSource the order source
     * @param offset      the offset
     * @return the lower order
     */
    public static int lower(Integer orderSource, int offset) {
        if (offset <= 0) {
            throw new IllegalArgumentException("offset must be greater than 0");
        }

        if (orderSource == null) {
            orderSource = Ordered.LOWEST_PRECEDENCE;
        }

        if (Ordered.LOWEST_PRECEDENCE - offset < orderSource) {
            return Ordered.LOWEST_PRECEDENCE;
        }

        return orderSource + offset;
    }

    /**
     * Higher.
     *
     * @param orderSource the order source
     * @param offset      the offset
     * @return the higher order
     */
    public static int higher(Integer orderSource, int offset) {
        if (offset <= 0) {
            throw new IllegalArgumentException("offset must be greater than 0");
        }

        if (orderSource == null) {
            orderSource = Ordered.LOWEST_PRECEDENCE;
        }

        if (Ordered.HIGHEST_PRECEDENCE + offset > orderSource) {
            return Ordered.HIGHEST_PRECEDENCE;
        }

        return orderSource - offset;
    }
}
