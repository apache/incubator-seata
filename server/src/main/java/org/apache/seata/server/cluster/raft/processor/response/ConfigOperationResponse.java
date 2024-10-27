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
package org.apache.seata.server.cluster.raft.processor.response;

import java.io.Serializable;

public class ConfigOperationResponse implements Serializable {
    private static final long serialVersionUID = -1439073440621259777L;

    private Object result;
    private boolean success;
    private String errMsg;

    public ConfigOperationResponse() {
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrMsg() {
        return errMsg;
    }

    public void setErrMsg(String errMsg) {
        this.errMsg = errMsg;
    }

    public static ConfigOperationResponse success() {
        ConfigOperationResponse response = new ConfigOperationResponse();
        response.setSuccess(true);
        return response;
    }

    public static ConfigOperationResponse success(Object result) {
        ConfigOperationResponse response = success();
        response.setResult(result);
        return response;
    }

    public static ConfigOperationResponse fail() {
        ConfigOperationResponse response = new ConfigOperationResponse();
        response.setSuccess(false);
        return response;
    }

    public static ConfigOperationResponse fail(String errMsg) {
        ConfigOperationResponse response = fail();
        response.setErrMsg(errMsg);
        return response;
    }
}
