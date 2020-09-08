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
    public static Integer getOrder(Object obj) {
        if (obj instanceof Ordered) {
            return ((Ordered) obj).getOrder();
        }

        return OrderUtils.getOrder(obj.getClass());
    }
}
