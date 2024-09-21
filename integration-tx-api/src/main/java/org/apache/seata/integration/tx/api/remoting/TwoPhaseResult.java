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
package org.apache.seata.integration.tx.api.remoting;

import org.apache.seata.common.util.StringUtils;

/**
 * the TCC method result
 *
 */
public class TwoPhaseResult {

    /**
     * is Success ?
     */
    private boolean isSuccess;

    /**
     * result message
     */
    private String message;

    /**
     * Instantiates a new Two phase result.
     *
     * @param isSuccess the is success
     * @param msg       the msg
     */
    public TwoPhaseResult(boolean isSuccess, String msg) {
        this.isSuccess = isSuccess;
        this.message = msg;
    }

    /**
     * Is success boolean.
     *
     * @return the boolean
     */
    public boolean isSuccess() {
        return isSuccess;
    }

    /**
     * Sets success.
     *
     * @param isSuccess the is success
     */
    public void setSuccess(boolean isSuccess) {
        this.isSuccess = isSuccess;
    }

    /**
     * Gets message.
     *
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets message.
     *
     * @param msg the message
     */
    public void setMessage(String msg) {
        this.message = msg;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        sb.append("isSuccess:").append(isSuccess);
        if (StringUtils.isNotBlank(message)) {
            sb.append(", msg:").append(message);
        }
        sb.append("]");
        return sb.toString();
    }
}
