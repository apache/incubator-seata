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
package io.seata.rm.tcc.api;

import io.seata.core.model.RollbackType;

/**
 * The enum of tcc two phase rollback type.
 *
 * @author PancrasL
 */
public enum TCCRollbackType {

    /**
     * The sync rollback.
     */
    SyncRollback,

    /**
     * The async rollback.
     */
    AsyncRollback,

    ;

    /**
     * Gets the corresponding RollbackType
     *
     * @return the rollback type
     */
    public RollbackType getRollbackType() {
        for (RollbackType rollbackType : RollbackType.values()) {
            if (rollbackType.name().equals(this.name())) {
                return rollbackType;
            }
        }
        throw new RuntimeException("The " + TCCRollbackType.class.getSimpleName() + " name '" + this.name() + "' is incorrect");
    }
}
