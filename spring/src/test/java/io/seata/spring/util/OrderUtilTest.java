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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.Ordered;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The type OrderUtil test
 * @author wang.liang
 */
public class OrderUtilTest {

    @Test
    public void test_getOrder() {
        // get from Ordered
        MockOrdered ordered = new MockOrdered(100);
        assertThat(OrderUtil.getOrder(ordered)).isEqualTo(100);

        // get from Annotation @Order(xxx)
        assertThat(OrderUtil.getOrder(new MockAnnotationOrdered())).isEqualTo(1);

        // no order
        assertThat(OrderUtil.getOrder(new Object())).isNull();
    }

    @Test
    public void test_lowerThen() {
        assertThat(OrderUtil.lowerThan(Ordered.LOWEST_PRECEDENCE, Ordered.HIGHEST_PRECEDENCE)).isTrue();
        assertThat(OrderUtil.lowerThan(1, 0)).isTrue();
    }

    @Test
    public void test_higherThen() {
        assertThat(OrderUtil.higherThan(Ordered.HIGHEST_PRECEDENCE, Ordered.LOWEST_PRECEDENCE)).isTrue();
        assertThat(OrderUtil.higherThan(0, 1)).isTrue();
    }

    @Test
    public void test_lower() {
        assertThat(OrderUtil.lower(1, 1)).isEqualTo(2);
        assertThat(OrderUtil.lower(Ordered.LOWEST_PRECEDENCE - 1, 2)).isEqualTo(Ordered.LOWEST_PRECEDENCE);
        assertThat(OrderUtil.lower(Ordered.LOWEST_PRECEDENCE, 1)).isEqualTo(Ordered.LOWEST_PRECEDENCE);

        Assertions.assertThrows(IllegalArgumentException.class, () -> OrderUtil.lower(1, -1));
    }

    @Test
    public void test_higher() {
        assertThat(OrderUtil.higher(1, 1)).isEqualTo(0);
        assertThat(OrderUtil.higher(Ordered.HIGHEST_PRECEDENCE + 1, 2)).isEqualTo(Ordered.HIGHEST_PRECEDENCE);
        assertThat(OrderUtil.higher(Ordered.HIGHEST_PRECEDENCE, 1)).isEqualTo(Ordered.HIGHEST_PRECEDENCE);

        Assertions.assertThrows(IllegalArgumentException.class, () -> OrderUtil.higher(1, -1));
    }
}
