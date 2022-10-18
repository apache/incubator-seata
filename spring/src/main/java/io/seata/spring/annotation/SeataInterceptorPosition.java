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
package io.seata.spring.annotation;

/**
 * The enum Seata interceptor position.
 *
 * @author wang.liang
 */
public enum SeataInterceptorPosition {

    /**
     * Any position.
     */
    Any,

    /**
     * Must be before/higherThan/outsideOf TransactionInterceptor.</br>
     * The SeataInterceptor's order must be smaller than TransactionInterceptor's order.
     */
    BeforeTransaction,

    /**
     * Must be after/lowerThan/insideOf TransactionInterceptor.</br>
     * The SeataInterceptor's order must be bigger than TransactionInterceptor's order.
     */
    AfterTransaction
}
