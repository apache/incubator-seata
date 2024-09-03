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
package io.seata.saga.statelang.domain;

/**
 * Recover Strategy
 */
@Deprecated
public enum RecoverStrategy {

    /**
     * Compensate
     */
    Compensate,

    /**
     * Forward
     */
    Forward;

    public static RecoverStrategy wrap(org.apache.seata.saga.statelang.domain.RecoverStrategy target) {
        if (target == null) {
            return null;
        }
        switch (target) {
            case Compensate:
                return Compensate;
            case Forward:
                return Forward;
            default:
                throw new IllegalArgumentException("Cannot convert " + target.name());
        }
    }

    public org.apache.seata.saga.statelang.domain.RecoverStrategy unwrap() {
        switch (this) {
            case Compensate:
                return org.apache.seata.saga.statelang.domain.RecoverStrategy.Compensate;
            case Forward:
                return org.apache.seata.saga.statelang.domain.RecoverStrategy.Forward;
            default:
                throw new IllegalArgumentException("Cannot convert " + this.name());
        }
    }
}
