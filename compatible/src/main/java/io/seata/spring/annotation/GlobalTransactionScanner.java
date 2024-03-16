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
package io.seata.spring.annotation;

import org.apache.seata.tm.api.FailureHandler;

public class GlobalTransactionScanner extends org.apache.seata.spring.annotation.GlobalTransactionScanner {

    public GlobalTransactionScanner(String txServiceGroup) {
        super(txServiceGroup);
    }

    public GlobalTransactionScanner(String txServiceGroup, int mode) {
        super(txServiceGroup, mode);
    }

    public GlobalTransactionScanner(String applicationId, String txServiceGroup) {
        super(applicationId, txServiceGroup);
    }

    public GlobalTransactionScanner(String applicationId, String txServiceGroup, int mode) {
        super(applicationId, txServiceGroup, mode);
    }

    public GlobalTransactionScanner(String applicationId, String txServiceGroup, FailureHandler failureHandlerHook) {
        super(applicationId, txServiceGroup, failureHandlerHook);
    }

    public GlobalTransactionScanner(String applicationId, String txServiceGroup, int mode, FailureHandler failureHandlerHook) {
        super(applicationId, txServiceGroup, mode, failureHandlerHook);
    }
}
