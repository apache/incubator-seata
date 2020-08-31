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

import org.aopalliance.aop.Advice;
import org.springframework.aop.Advisor;
import org.springframework.core.Ordered;

/**
 * @author wang.liang
 */
public class MockAdvisor implements Advisor, Ordered {

    private Integer order;
    private Advice advice;

    public MockAdvisor(Integer order, Advice advice) {
        this.order = order;
        this.advice = advice;
    }

    @Override
    public int getOrder() {
        return order;
    }

    @Override
    public Advice getAdvice() {
        return advice;
    }

    @Override
    public boolean isPerInstance() {
        return false;
    }
}
