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
package org.apache.seata.rm.datasource.xa;

import org.apache.seata.rm.datasource.exec.StatementCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.sql.Statement;

/**
 * The type Execute template.
 *
 */
public class ExecuteTemplateXA {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecuteTemplateXA.class);

    public static <T, S extends Statement> T execute(AbstractConnectionProxyXA connectionProxyXA,
                                                     StatementCallback<T, S> statementCallback,
                                                     S targetStatement,
                                                     Object... args) throws SQLException {
        boolean autoCommitStatus = connectionProxyXA.getAutoCommit();
        if (autoCommitStatus) {
            // XA Start
            connectionProxyXA.setAutoCommit(false);
        }
        try {
            T res = null;
            try {
                // execute SQL
                res = statementCallback.execute(targetStatement, args);

            } catch (Throwable ex) {
                if (autoCommitStatus) {
                    // XA End & Rollback
                    try {
                        connectionProxyXA.rollback();
                    } catch (SQLException sqle) {
                        // log and ignore the rollback failure.
                        LOGGER.warn(
                            "Failed to rollback xa branch of " + connectionProxyXA.xid +
                                "(caused by SQL execution failure(" + ex.getMessage() + ") since " + sqle.getMessage(),
                            sqle);
                    }
                }

                if (ex instanceof SQLException) {
                    throw ex;
                } else {
                    throw new SQLException(ex);
                }

            }
            if (autoCommitStatus) {
                try {
                    // XA End & Prepare
                    connectionProxyXA.commit();
                } catch (Throwable ex) {
                    LOGGER.warn(
                        "Failed to commit xa branch of " + connectionProxyXA.xid + ") since " + ex.getMessage(),
                        ex);
                    // XA End & Rollback
                    if (!(ex instanceof SQLException) || !AbstractConnectionProxyXA.SQLSTATE_XA_NOT_END.equalsIgnoreCase(((SQLException) ex).getSQLState())) {
                        try {
                            connectionProxyXA.rollback();
                        } catch (SQLException sqle) {
                            // log and ignore the rollback failure.
                            LOGGER.warn(
                                "Failed to rollback xa branch of " + connectionProxyXA.xid +
                                    "(caused by commit failure(" + ex.getMessage() + ") since " + sqle.getMessage(),
                                sqle);
                        }
                    }

                    if (ex instanceof SQLException) {
                        throw ex;
                    } else {
                        throw new SQLException(ex);
                    }

                }
            }
            return res;
        } finally {
            if (autoCommitStatus) {
                connectionProxyXA.setAutoCommit(true);
            }

        }
    }
}
