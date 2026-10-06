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
package org.apache.seata.common.thread;

import org.apache.seata.common.util.StringUtils;

/**
 * Supported Seata thread pool modes.
 */
public enum ThreadPoolType {
    /**
     * Automatic selection: uses virtual threads when running on JDK 25 or later (with the loom extension
     * present), otherwise falls back to platform threads.
     */
    AUTO("auto"),
    /**
     * Always uses platform (OS) threads regardless of the JDK version.
     */
    PLATFORM("platform"),
    /**
     * Prefers virtual threads when running on JDK 21 or later (with the loom extension present),
     * otherwise falls back to platform threads.
     */
    VIRTUAL("virtual");

    private final String code;

    ThreadPoolType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static ThreadPoolType from(String code) {
        if (StringUtils.isBlank(code)) {
            return AUTO;
        }
        for (ThreadPoolType threadPoolType : values()) {
            if (threadPoolType.code.equalsIgnoreCase(code)) {
                return threadPoolType;
            }
        }
        return AUTO;
    }
}
