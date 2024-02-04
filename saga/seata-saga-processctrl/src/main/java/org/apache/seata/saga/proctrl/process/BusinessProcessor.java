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
package org.apache.seata.saga.proctrl.process;

import org.apache.seata.common.exception.FrameworkException;
import org.apache.seata.saga.proctrl.ProcessContext;

/**
 * Business logic processor
 *
 */
public interface BusinessProcessor {

    /**
     * process business logic
     *
     * @param context
     * @throws FrameworkException
     */
    void process(ProcessContext context) throws FrameworkException;

    /**
     * route
     *
     * @param context
     * @throws FrameworkException
     */
    void route(ProcessContext context) throws FrameworkException;
}
