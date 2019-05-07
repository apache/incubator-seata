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
package io.seata.core.rpc.netty;

import io.seata.core.protocol.AbstractMessage;

/**
 * The type Netty pool key.
 *
 * @author jimin.jm @alibaba-inc.com
 * @date 2018 /11/29
 */
public class NettyPoolKey {

    private TransactionRole transactionRole;
    private String address;
    private AbstractMessage message;

    /**
     * Instantiates a new Netty pool key.
     *
     * @param transactionRole the client role
     * @param address         the address
     */
    public NettyPoolKey(TransactionRole transactionRole, String address) {
        this.transactionRole = transactionRole;
        this.address = address;
    }

    /**
     * Instantiates a new Netty pool key.
     *
     * @param transactionRole the client role
     * @param address         the address
     * @param message         the message
     */
    public NettyPoolKey(TransactionRole transactionRole, String address, AbstractMessage message) {
        this.transactionRole = transactionRole;
        this.address = address;
        this.message = message;
    }

    /**
     * Gets get client role.
     *
     * @return the get client role
     */
    public TransactionRole getTransactionRole() {
        return transactionRole;
    }

    /**
     * Sets set client role.
     *
     * @param transactionRole the client role
     * @return the client role
     */
    public NettyPoolKey setTransactionRole(TransactionRole transactionRole) {
        this.transactionRole = transactionRole;
        return this;
    }

    /**
     * Gets get address.
     *
     * @return the get address
     */
    public String getAddress() {
        return address;
    }

    /**
     * Sets set address.
     *
     * @param address the address
     * @return the address
     */
    public NettyPoolKey setAddress(String address) {
        this.address = address;
        return this;
    }

    /**
     * Gets message.
     *
     * @return the message
     */
    public AbstractMessage getMessage() {
        return message;
    }

    /**
     * Sets message.
     *
     * @param message the message
     */
    public void setMessage(AbstractMessage message) {
        this.message = message;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("transactionRole:");
        sb.append(transactionRole.name());
        sb.append(",");
        sb.append("address:");
        sb.append(address);
        sb.append(",");
        sb.append("msg:< ");
        sb.append(message.toString());
        sb.append(" >");
        return sb.toString();
    }

    /**
     * The enum Client role.
     */
    public enum TransactionRole {

        /**
         * tm
         */
        TMROLE(1),
        /**
         * rm
         */
        RMROLE(2),
        /**
         * server
         */
        SERVERROLE(3);

        TransactionRole(int value) {
            this.value = value;
        }

        /**
         * Gets value.
         *
         * @return value value
         */
        public int getValue() {
            return value;
        }

        /**
         * 状态值
         */
        private int value;
    }
}
