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
package io.seata.integration.tx.api.fence.exception;

import io.seata.common.exception.FrameworkErrorCode;
import io.seata.common.exception.FrameworkException;

/**
 * Common Fence Exception
 *
 */
public class CommonFenceException extends FrameworkException {

    public CommonFenceException(FrameworkErrorCode err) {
        super(err);
    }

    public CommonFenceException(String msg) {
        super(msg);
    }

    public CommonFenceException(String msg, FrameworkErrorCode errCode) {
        super(msg, errCode);
    }

    public CommonFenceException(Throwable cause, String msg, FrameworkErrorCode errCode) {
        super(cause, msg, errCode);
    }

    public CommonFenceException(Throwable th) {
        super(th);
    }

    public CommonFenceException(Throwable th, String msg) {
        super(th, msg);
    }

}
