/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.rm.tcc.spring.tcc;

import org.apache.seata.rm.tcc.api.BusinessActionContext;
import org.apache.seata.rm.tcc.api.TwoPhaseBusinessAction;

/**
 * The interface Tcc action.
 *
 */
public interface TccAction {

    /**
     * Prepare boolean.
     *
     * @param actionContext the action context
     * @param i the
     * @return the boolean
     */
    @TwoPhaseBusinessAction(name = "tccActionForSpringTest" , commitMethod = "commit", rollbackMethod = "rollback")
    boolean prepare(BusinessActionContext actionContext, int i);

    /**
     * Commit boolean.
     *
     * @param actionContext the action context
     * @return the boolean
     */
    boolean commit(BusinessActionContext actionContext);

    /**
     * Rollback boolean.
     *
     * @param actionContext the action context
     * @return the boolean
     */
    boolean rollback(BusinessActionContext actionContext);
}
