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
package io.seata.tm.api.transaction;

/**
 * Propagation level of global transactions.
 *
 * @author haozhibei
 * @see io.seata.spring.annotation.GlobalTransactional
 * @see io.seata.tm.api.TransactionalTemplate
 */
public enum Propagation {
    /**
     * The REQUIRED.
     * <p>
     * If transaction is existing, execute with current transaction,
     * else execute with new transaction.
     * </p>
     * <pre>
     *     if (tx == null) {
     *         tx = beginNewTransaction(); // begin new transaction, is not existing
     *         try {
     *             Object rs = business.execute(); // execute with new transaction
     *             commitTransaction(tx);
     *             return rs;
     *         } catch (Exception ex) {
     *             rollbackTransaction(tx);
     *             throw ex;
     *         }
     *     } else {
     *         return business.execute(); // execute with current transaction
     *     }
     * </pre>
     */
    REQUIRED,

    /**
     * The REQUIRES_NEW.
     * <p>
     * If transaction is existing, suspend it, and then execute business with new transaction.
     * </p>
     * <pre>
     *     try {
     *         if (tx != null) {
     *             suspendedResource = suspendTransaction(tx); // suspend current transaction
     *         }
     *         tx = beginNewTransaction(); // begin new transaction
     *         try {
     *             Object rs = business.execute(); // execute with new transaction
     *             commitTransaction(tx);
     *             return rs;
     *         } catch (Exception ex) {
     *             rollbackTransaction(tx);
     *             throw ex;
     *         }
     *     } finally {
     *         if (suspendedResource != null) {
     *             resumeTransaction(suspendedResource); // resume transaction
     *         }
     *     }
     * </pre>
     */
    REQUIRES_NEW,

    /**
     * The NOT_SUPPORTED.
     * <p>
     * If transaction is existing, suspend it, and then execute business without transaction.
     * </p>
     * <pre>
     *     try {
     *         if (tx != null) {
     *             suspendedResource = suspendTransaction(tx); // suspend current transaction
     *         }
     *         return business.execute(); // execute without transaction
     *     } finally {
     *         if (suspendedResource != null) {
     *             resumeTransaction(suspendedResource); // resume transaction
     *         }
     *     }
     * </pre>
     */
    NOT_SUPPORTED,

    /**
     * The SUPPORTS.
     * <p>
     * If transaction is not existing, execute without global transaction,
     * else execute business with current transaction.
     * </p>
     * <pre>
     *     if (tx != null) {
     *         return business.execute(); // execute with current transaction
     *     } else {
     *         return business.execute(); // execute without transaction
     *     }
     * </pre>
     */
    SUPPORTS,

    /**
     * The NEVER.
     * <p>
     * If transaction is existing, throw exception,
     * else execute business without transaction.
     * </p>
     * <pre>
     *     if (tx != null) {
     *         throw new TransactionException("existing transaction");
     *     }
     *     return business.execute(); // execute without transaction
     * </pre>
     */
    NEVER,

    /**
     * The MANDATORY.
     * <p>
     * If transaction is not existing, throw exception,
     * else execute business with current transaction.
     * </p>
     * <pre>
     *     if (tx == null) {
     *         throw new TransactionException("not existing transaction");
     *     }
 *         return business.execute(); // execute with current transaction
     * </pre>
     */
    MANDATORY
}
