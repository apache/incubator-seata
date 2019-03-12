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
package com.alibaba.fescar.core.exception;

import com.alibaba.fescar.core.protocol.ResultCode;
import com.alibaba.fescar.core.protocol.transaction.AbstractTransactionRequest;
import com.alibaba.fescar.core.protocol.transaction.AbstractTransactionResponse;

/**
 * The type Abstract exception handler.
 */
public abstract class AbstractExceptionHandler {

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
    }

    /**
     * Exception handle template.
     *
     * @param callback the callback
     * @param request  the request
     * @param response the response
     */
    public void exceptionHandleTemplate(Callback callback, AbstractTransactionRequest request,
                                        AbstractTransactionResponse response) {
        try {
            callback.execute(request, response);
            response.setResultCode(ResultCode.Success);

        } catch (TransactionException tex) {
            response.setTransactionExceptionCode(tex.getCode());
            response.setResultCode(ResultCode.Failed);
            response.setMsg("TransactionException[" + tex.getMessage() + "]");

        } catch (RuntimeException rex) {
            response.setResultCode(ResultCode.Failed);
            response.setMsg("RuntimeException[" + rex.getMessage() + "]");
        }
    }

}
