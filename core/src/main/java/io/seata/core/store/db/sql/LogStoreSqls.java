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
package io.seata.core.store.db.sql;

/**
 * Database log store sql
 * @author will
 */
public interface LogStoreSqls {

    /**
     * Get insert global transaction sql string.
     *
     * @param globalTable the global table
     * @param dbType      the db type
     * @return the string
     */
    String getInsertGlobalTransactionSQL(String globalTable, String dbType);

    /**
     * Get update global transaction status sql string.
     *
     * @param globalTable the global table
     * @param dbType      the db type
     * @return the string
     */
    String getUpdateGlobalTransactionStatusSQL(String globalTable, String dbType);

    /**
     * Get delete global transaction sql string.
     *
     * @param globalTable the global table
     * @param dbType      the db type
     * @return the string
     */
    String getDeleteGlobalTransactionSQL(String globalTable, String dbType);

    /**
     * Get query global transaction sql string.
     *
     * @param globalTable the global table
     * @param dbType      the db type
     * @return the string
     */
    String getQueryGlobalTransactionSQL(String globalTable, String dbType);

    /**
     * Get query global transaction sql by transaction id string.
     *
     * @param globalTable the global table
     * @param dbType      the db type
     * @return the string
     */
    String getQueryGlobalTransactionSQLByTransactionId(String globalTable, String dbType);

    /**
     * Get query global transaction sql by status string.
     *
     * @param globalTable       the global table
     * @param dbType            the db type
     * @param paramsPlaceHolder the params place holder
     * @return the string
     */
    String getQueryGlobalTransactionSQLByStatus(String globalTable, String dbType,
                                                              String paramsPlaceHolder);

    /**
     * Get query global transaction for recovery sql string.
     *
     * @param globalTable the global table
     * @param dbType      the db type
     * @return the string
     */
    String getQueryGlobalTransactionForRecoverySQL(String globalTable, String dbType);

    /**
     * Get insert branch transaction sql string.
     *
     * @param branchTable the branch table
     * @param dbType      the db type
     * @return the string
     */
    String getInsertBranchTransactionSQL(String branchTable, String dbType);

    /**
     * Get update branch transaction status sql string.
     *
     * @param branchTable the branch table
     * @param dbType      the db type
     * @return the string
     */
    String getUpdateBranchTransactionStatusSQL(String branchTable, String dbType);

    /**
     * Get delete branch transaction by branch id sql string.
     *
     * @param branchTable the branch table
     * @param dbType      the db type
     * @return the string
     */
    String getDeleteBranchTransactionByBranchIdSQL(String branchTable, String dbType);

    /**
     * Get delete branch transaction by x id string.
     *
     * @param branchTable the branch table
     * @param dbType      the db type
     * @return the string
     */
    String getDeleteBranchTransactionByXId(String branchTable, String dbType);

    /**
     * Get query branch transaction string.
     *
     * @param branchTable the branch table
     * @param dbType      the db type
     * @return the string
     */
    String getQueryBranchTransaction(String branchTable, String dbType);

    /**
     * Get query branch transaction string.
     *
     * @param branchTable the branch table
     * @param dbType      the db type
     * @param paramsPlaceHolder the params place holder
     * @return the string
     */
    String getQueryBranchTransaction(String branchTable, String dbType,
                                                   String paramsPlaceHolder);

    /**
     * Gets query global max.
     *
     * @param globalTable the global table
     * @param dbType      the db type
     * @return the query global max
     */
    String getQueryGlobalMax(String globalTable, String dbType);

    /**
     * Gets query branch max.
     *
     * @param branchTable the branch table
     * @param dbType      the db type
     * @return the query branch max
     */
    String getQueryBranchMax(String branchTable, String dbType);
}
