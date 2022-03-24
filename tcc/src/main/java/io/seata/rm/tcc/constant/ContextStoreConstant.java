/*
 *  Copyright 1999-2019 Seata.io Group.
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
package io.seata.rm.tcc.constant;

/**
 * the constants of businessActionContext store
 */
public interface ContextStoreConstant {

    /**
     * the constant STORE_TYPE_TC
     */
    String STORE_TYPE_TC = "TC";

    /**
     * the constant STORE_TYPE_FENCE
     */
    String STORE_TYPE_FENCE = "FENCE";
}
