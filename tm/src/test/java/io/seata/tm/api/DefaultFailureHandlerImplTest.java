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
package io.seata.tm.api;

import io.seata.core.exception.TransactionException;
import io.seata.core.model.GlobalStatus;
import org.junit.Test;

import java.util.Random;

/**
 * @author XCXCXCXCX
 */
public class DefaultFailureHandlerImplTest {

    @Test
    public void checkTimerTaskTest() throws InterruptedException {
        FailureHandler defaultFailureHandler = new DefaultFailureHandlerImpl();
        for(int i = 0; i < 10; i++){
            defaultFailureHandler.onCommitFailure(new GlobalTransaction() {
                @Override
                public void begin() throws TransactionException {

                }

                @Override
                public void begin(int timeout) throws TransactionException {

                }

                @Override
                public void begin(int timeout, String name) throws TransactionException {

                }

                @Override
                public void commit() throws TransactionException {

                }

                @Override
                public void rollback() throws TransactionException {

                }

                @Override
                public GlobalStatus getStatus() throws TransactionException {
                    GlobalStatus result = GlobalStatus.CommitRetrying;
                    if(new Random().nextInt(2) == 1){
                        result = GlobalStatus.Committed;
                    }
                    return result;
                }

                @Override
                public String getXid() {
                    return null;
                }
            }, null);
        }
        Thread.sleep(11000);
    }
}
