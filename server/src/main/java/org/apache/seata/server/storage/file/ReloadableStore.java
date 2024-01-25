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
package org.apache.seata.server.storage.file;

import java.util.List;

/**
 * The interface Reloadable store.
 *
 */
public interface ReloadableStore {

    /**
     * Read write store.
     *
     * @param readSize  the read size
     * @param isHistory the is history
     * @return the list
     */
    List<TransactionWriteStore> readWriteStore(int readSize, boolean isHistory);

    /**
     * Has remaining boolean.
     *
     * @param isHistory the is history
     * @return the boolean
     */
    boolean hasRemaining(boolean isHistory);


}
