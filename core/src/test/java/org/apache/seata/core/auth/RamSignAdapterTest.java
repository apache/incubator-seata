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
package org.apache.seata.core.auth;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * The RamSignAdapter Test
 */
public class RamSignAdapterTest {
    @Test
    public void testGetDateSigningKey() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        String secret = "mySecret";
        String date = "20220101";
        String signMethod = "HmacSHA256";
        byte[] expectArray = new byte[]{-96, 108, 42, 75, -59, 121, -63, 108, -3, -126, 67, 3, 118, 2, 39, 59, -68, -37, -98, 122, -25, -120, 77, 56, -70, 24, -115, 33, 125, -128, -10, -26};

        RamSignAdapter adapter = new RamSignAdapter();
        // Use reflection to access the private method
        Method getDateSigningKeyMethod = RamSignAdapter.class.getDeclaredMethod("getDateSigningKey", String.class, String.class, String.class);
        getDateSigningKeyMethod.setAccessible(true);
        byte[] signingKey = (byte[]) getDateSigningKeyMethod.invoke(adapter, secret, date, signMethod);
        Assertions.assertEquals(32, signingKey.length);
        Assertions.assertArrayEquals(expectArray, signingKey);
    }

    @Test
    public void testGetRegionSigningKey() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        String secret = "mySecret";
        String date = "20220101";
        String region = "cn-beijing";
        String signMethod = "HmacSHA256";
        byte[] expectArray = new byte[]{-40, 5, 2, 41, -48, 82, 10, -102, 125, -24, -44, -83, 127, 6, -85, 93, -26, 88, -88, 65, 56, 79, -5, -66, 65, -106, 19, -64, -85, 103, -32, 110};

        RamSignAdapter adapter = new RamSignAdapter();
        // Use reflection to access the private method
        Method getRegionSigningKeyMethod = RamSignAdapter.class.getDeclaredMethod("getRegionSigningKey", String.class, String.class, String.class, String.class);
        getRegionSigningKeyMethod.setAccessible(true);
        byte[] signingKey = (byte[]) getRegionSigningKeyMethod.invoke(adapter, secret, date, region, signMethod);
        Assertions.assertEquals(32, signingKey.length);
        Assertions.assertArrayEquals(expectArray, signingKey);
    }

    @Test
    public void testGetProductSigningKey() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        String secret = "mySecret";
        String date = "20220101";
        String region = "cn-beijing";
        String productCode = "seata";
        String signMethod = "HmacSHA256";
        byte[] expectArray = new byte[]{62, 98, -65, 30, -8, -3, 66, -111, 0, 123, 126, 78, -30, -74, 55, -79, 101, -18, -97, -5, 78, -19, -17, 0, 88, 30, -92, 108, 103, 87, 49, -22};

        RamSignAdapter adapter = new RamSignAdapter();
        Method getProductSigningKeyMethod = RamSignAdapter.class.getDeclaredMethod("getProductSigningKey", String.class, String.class, String.class, String.class, String.class);
        getProductSigningKeyMethod.setAccessible(true);
        byte[] signingKey = (byte[]) getProductSigningKeyMethod.invoke(adapter, secret, date, region, productCode, signMethod);
        Assertions.assertEquals(32, signingKey.length);
        Assertions.assertArrayEquals(expectArray, signingKey);
    }

    @Test
    public void testGetRamSign() {
        String encryptText = "testGroup,127.0.0.1,1702564471650";
        String encryptKey = "exampleEncryptKey";
        String expectedSign = "6g9nMk6BRLFxl7bf5ZfWaEZvGdho3JBmwvx5rqgSUCE=";
        String actualSign = RamSignAdapter.getRamSign(encryptText, encryptKey);
        // Assert the generated sign matches the expected sign
        Assertions.assertEquals(expectedSign, actualSign);
    }
}
