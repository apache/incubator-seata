/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
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
package com.alibaba.fescar.rm.tcc;

import java.util.HashMap;
import java.util.Map;

/**
 * the TCC method result
 * @author zhangsen
 */
public class TwoPhaseResult {

    /**
     * is Success ?
     */
    private boolean             isSuccess = false;

    /**
     * result message
     */
    private String              msg;

    private Map<String, Object> context = new HashMap<String, Object>();

    public TwoPhaseResult(boolean isSuccess, String msg) {
        this.isSuccess = isSuccess;
        this.msg = msg;
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    public void setSuccess(boolean isSuccess) {
        this.isSuccess = isSuccess;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public Map<String, Object> getContext() {
        return context;
    }

    public Object getContext(String key) {
        return context.get(key);
    }

    public void setContext(Map<String, Object> context) {
        this.context = context;
    }

    public void addContext(String key, Object value) {
        context.put(key, value);
    }

    public String toString() {
        return String.valueOf(isSuccess);
    }
}
