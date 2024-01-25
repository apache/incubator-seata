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
package org.apache.seata.core.exception;

import java.util.Objects;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.core.constants.ConfigurationKeys;
import org.apache.seata.core.model.LockStatus;
import org.apache.seata.core.protocol.ResultCode;
import org.apache.seata.core.protocol.transaction.AbstractTransactionRequest;
import org.apache.seata.core.protocol.transaction.AbstractTransactionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type Abstract exception handler.
 *
 */
public abstract class AbstractExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractExceptionHandler.class);

    /**
     * The constant CONFIG.
     */
    protected static final Configuration CONFIG = ConfigurationFactory.getInstance();

    /**
     * The interface Callback.
     *
     * @param <T> the type parameter
     * @param <S> the type parameter
     */
    public interface Callback<T extends AbstractTransactionRequest, S extends AbstractTransactionResponse> {
        /**
         * Execute.
         *
         * @param request  the request
         * @param response the response
         * @throws TransactionException the transaction exception
         */
        void execute(T request, S response) throws TransactionException;

        /**
         * On success.
         *
         * @param request  the request
         * @param response the response
         */
        void onSuccess(T request, S response);

        /**
         * onTransactionException
         *
         * @param request   the request
         * @param response  the response
         * @param exception the exception
         */
        void onTransactionException(T request, S response, TransactionException exception);

        /**
         * on other exception
         *
         * @param request   the request
         * @param response  the response
         * @param exception the exception
         */
        void onException(T request, S response, Exception exception);

    }

    /**
     * The type Abstract callback.
     *
     * @param <T> the type parameter
     * @param <S> the type parameter
     */
    public abstract static class AbstractCallback<T extends AbstractTransactionRequest, S extends AbstractTransactionResponse>
        implements Callback<T, S> {

        @Override
        public void onSuccess(T request, S response) {
            response.setResultCode(ResultCode.Success);
        }

        @Override
        public void onTransactionException(T request, S response,
            TransactionException tex) {
            response.setTransactionExceptionCode(tex.getCode());
            response.setResultCode(ResultCode.Failed);
            response.setMsg("TransactionException[" + tex.getMessage() + "]");
        }

        @Override
        public void onException(T request, S response, Exception rex) {
            response.setResultCode(ResultCode.Failed);
            response.setMsg("RuntimeException[" + rex.getMessage() + "]");
        }
    }

    /**
     * Exception handle template.
     *
     * @param <T>      the type parameter
     * @param <S>      the type parameter
     * @param callback the callback
     * @param request  the request
     * @param response the response
     */
    public <T extends AbstractTransactionRequest, S extends AbstractTransactionResponse> void exceptionHandleTemplate(Callback<T, S> callback, T request, S response) {
        try {
            callback.execute(request, response);
            callback.onSuccess(request, response);
        } catch (TransactionException tex) {
            if (Objects.equals(TransactionExceptionCode.LockKeyConflict, tex.getCode())) {
                LOGGER.error("this request cannot acquire global lock, you can let Seata retry by setting config [{}] = false or manually retry by yourself. request: {}",
                        ConfigurationKeys.CLIENT_LOCK_RETRY_POLICY_BRANCH_ROLLBACK_ON_CONFLICT, request);
            } else if (Objects.equals(TransactionExceptionCode.LockKeyConflictFailFast, tex.getCode())) {
                LOGGER.error("this request cannot acquire global lock, decide fail-fast because LockStatus is {}. request: {}",
                        LockStatus.Rollbacking, request);
            } else {
                LOGGER.error("Catch TransactionException while do RPC, request: {}", request, tex);
            }
            callback.onTransactionException(request, response, tex);
        } catch (RuntimeException rex) {
            LOGGER.error("Catch RuntimeException while do RPC, request: {}", request, rex);
            callback.onException(request, response, rex);
        }
    }

}
