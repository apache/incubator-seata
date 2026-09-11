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
package io.seata.core.model;

import io.seata.core.exception.TransactionExceptionCode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Montos
 */
public class TransactionExceptionCodeTest {
    private static final int BEGIN_CODE = 1;
    private static final int NONE = 99;
    private static final int MIN_CODE = 0;
    private static final int Max_CODE = 18;

    @Test
    public void testGetCode() {
        int code = TransactionExceptionCode.BeginFailed.ordinal();
        Assertions.assertEquals(code, BEGIN_CODE);
    }

    @Test
    public void testGetWithByte() {
        TransactionExceptionCode branchStatus = TransactionExceptionCode.get((byte) BEGIN_CODE);
        Assertions.assertEquals(branchStatus, TransactionExceptionCode.BeginFailed);
    }

    @Test
    public void testGetWithInt() {
        TransactionExceptionCode branchStatus = TransactionExceptionCode.get(BEGIN_CODE);
        Assertions.assertEquals(branchStatus, TransactionExceptionCode.BeginFailed);
    }

    @Test
    public void testGetException() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> TransactionExceptionCode.get(NONE));
    }

    @Test
    public void testGetByCode() {
        TransactionExceptionCode transactionExceptionCodeOne = TransactionExceptionCode.get(MIN_CODE);
        Assertions.assertEquals(transactionExceptionCodeOne, TransactionExceptionCode.Unknown);

        TransactionExceptionCode transactionExceptionCodeTwo = TransactionExceptionCode.get(Max_CODE);
        Assertions.assertEquals(transactionExceptionCodeTwo, TransactionExceptionCode.FailedStore);

        Assertions.assertThrows(IllegalArgumentException.class, () -> TransactionExceptionCode.get(NONE));
    }



}
