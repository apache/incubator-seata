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
package com.alibaba.fescar.server.store;

import java.util.List;

/**
 * The interface Transaction store manager.
 */
public interface TransactionStoreManager {

    /**
     * Write session boolean.
     *
     * @param logOperation the log operation
     * @param session      the session
     * @return the boolean
     */
    boolean writeSession(LogOperation logOperation, SessionStorable session);

    /**
     * Shutdown.
     */
    void shutdown();

    /**
     * Read write store from file list.
     *
     * @param readSize  the read size
     * @param isHistory the is history
     * @return the list
     */
    List<TransactionWriteStore> readWriteStoreFromFile(int readSize, boolean isHistory);

    /**
     * Has remaining boolean.
     *
     * @param isHistory the is history
     * @return the boolean
     */
    boolean hasRemaining(boolean isHistory);

    /**
     * The enum Log operation.
     */
    enum LogOperation {

        /**
         * Global add log operation.
         */
        GLOBAL_ADD((byte)1),
        /**
         * Global update log operation.
         */
        GLOBAL_UPDATE((byte)2),
        /**
         * Global remove log operation.
         */
        GLOBAL_REMOVE((byte)3),
        /**
         * Branch add log operation.
         */
        BRANCH_ADD((byte)4),
        /**
         * Branch update log operation.
         */
        BRANCH_UPDATE((byte)5),
        /**
         * Branch remove log operation.
         */
        BRANCH_REMOVE((byte)6);

        private byte code;

        LogOperation(byte code) {
            this.code = code;
        }

        /**
         * Gets code.
         *
         * @return the code
         */
        public byte getCode() {
            return this.code;
        }

        /**
         * Gets log operation by code.
         *
         * @param code the code
         * @return the log operation by code
         */
        public static LogOperation getLogOperationByCode(byte code) {
            LogOperation logOperation = null;
            switch (code) {
                case 1:
                    logOperation = LogOperation.GLOBAL_ADD;
                    break;
                case 2:
                    logOperation = LogOperation.GLOBAL_UPDATE;
                    break;
                case 3:
                    logOperation = LogOperation.GLOBAL_REMOVE;
                    break;
                case 4:
                    logOperation = LogOperation.BRANCH_ADD;
                    break;
                case 5:
                    logOperation = LogOperation.BRANCH_UPDATE;
                    break;
                case 6:
                    logOperation = LogOperation.BRANCH_REMOVE;
                    break;
                default:
            }
            return logOperation;
        }
    }
}
