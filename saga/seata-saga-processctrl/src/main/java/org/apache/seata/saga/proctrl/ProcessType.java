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
package org.apache.seata.saga.proctrl;

/**
 * Process type
 *
 */
public enum ProcessType {

    /**
     * SEATA State Language
     */
    STATE_LANG("STATE_LANG", "SEATA State Language");

    private String code;

    private String message;

    ProcessType(String code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * get enum by code
     *
     * @param code the type code
     * @return the process type
     */
    public static ProcessType getEnumByCode(String code) {
        for (ProcessType codetmp : ProcessType.values()) {
            if (codetmp.getCode().equalsIgnoreCase(code)) {
                return codetmp;
            }
        }
        return null;
    }

    /**
     * get code
     *
     * @return the process type code
     */
    public String getCode() {
        return code;
    }

    /**
     * get message
     *
     * @return the process type message
     */
    public String getMessage() {
        return message;
    }
}
