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
package com.alibaba.fescar.core.protocol;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.alibaba.fastjson.JSON;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The type Message future test.
 *
 * @author guoyao
 * @date 2019 /3/2
 */
public class MessageFutureTest {

    private final boolean ASYNC_FIELD = false;
    private final String BODY_FIELD = "test_body";
    private final boolean HEART_BEAT_FIELD = true;
    private final boolean REQUEST_FIELD = false;
    private final long ID_FIELD = 100L;
    private final long TIME_OUT_FIELD = 100L;

    /**
     * Test field set get.
     */
    @Test
    public void testFieldSetGet() {
        String fromJson = "{\n" +
            "\t\"requestMessage\":{\n" +
            "\t\t\"async\":" + ASYNC_FIELD + ",\n" +
            "\t\t\"body\":\"" + BODY_FIELD + "\",\n" +
            "\t\t\"heartbeat\":" + HEART_BEAT_FIELD + ",\n" +
            "\t\t\"id\":" + ID_FIELD + ",\n" +
            "\t\t\"request\":" + REQUEST_FIELD + "\n" +
            "\t},\n" +
            "\t\"timeout\":" + TIME_OUT_FIELD + "\n" +
            "}";
        MessageFuture fromJsonFuture = JSON.parseObject(fromJson, MessageFuture.class);
        assertThat(fromJsonFuture.getTimeout()).isEqualTo(TIME_OUT_FIELD);
        MessageFuture toJsonFuture = new MessageFuture();
        toJsonFuture.setRequestMessage(buildRepcMessage());
        toJsonFuture.setTimeout(TIME_OUT_FIELD);
        String toJson = JSON.toJSONString(toJsonFuture, true);
        assertThat(toJson).isEqualTo(fromJson);
    }

    /**
     * Test is time out.
     *
     * @throws Exception the exception
     */
    @Test
    public void testIsTimeOut() throws Exception {
        MessageFuture messageFuture = new MessageFuture();
        messageFuture.setTimeout(TIME_OUT_FIELD);
        assertThat(messageFuture.isTimeout()).isFalse();
        Thread.sleep(TIME_OUT_FIELD + 1);
        assertThat(messageFuture.isTimeout()).isTrue();

    }

    /**
     * Test get no result with time out exception.
     *
     * @throws Exception the exception
     */
    @Test(expected = TimeoutException.class)
    public void testGetNoResultWithTimeOutException() throws Exception {
        MessageFuture messageFuture = new MessageFuture();
        messageFuture.setRequestMessage(buildRepcMessage());
        messageFuture.setTimeout(TIME_OUT_FIELD);
        messageFuture.get(TIME_OUT_FIELD, TimeUnit.MILLISECONDS);
    }

    /**
     * Test get has result with time out exception.
     *
     * @throws Exception the exception
     */
    @Test(expected = TimeoutException.class)
    public void testGetHasResultWithTimeOutException() throws Exception {
        MessageFuture messageFuture = new MessageFuture();
        messageFuture.setRequestMessage(buildRepcMessage());
        messageFuture.setTimeout(TIME_OUT_FIELD);
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        CountDownLatch downLatch = new CountDownLatch(1);
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    downLatch.await();
                    messageFuture.setResultMessage("has_result");
                } catch (InterruptedException e) {

                }
            }
        });
        messageFuture.get(TIME_OUT_FIELD, TimeUnit.MILLISECONDS);
        downLatch.countDown();
    }

    /**
     * Test get has result with run time exception.
     *
     * @throws Exception the exception
     */
    @Test(expected = RuntimeException.class)
    public void testGetHasResultWithRunTimeException() throws Exception {
        MessageFuture messageFuture = new MessageFuture();
        messageFuture.setRequestMessage(buildRepcMessage());
        messageFuture.setTimeout(TIME_OUT_FIELD);
        messageFuture.setResultMessage(new RuntimeException());
        messageFuture.get(TIME_OUT_FIELD, TimeUnit.MILLISECONDS);
    }

    /**
     * Test get has result with run time exception with message.
     *
     * @throws Exception the exception
     */
    @Test
    public void testGetHasResultWithRunTimeExceptionWithMessage() throws Exception {
        MessageFuture messageFuture = new MessageFuture();
        messageFuture.setRequestMessage(buildRepcMessage());
        messageFuture.setTimeout(TIME_OUT_FIELD);
        RuntimeException runtimeException = new RuntimeException("test_runtime");
        messageFuture.setResultMessage(runtimeException);
        try {
            messageFuture.get(TIME_OUT_FIELD, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            assertThat(e.getMessage()).isEqualTo(runtimeException.getMessage());
        }
    }

    /**
     * Test get has result with throwable.
     *
     * @throws Exception the exception
     */
    @Test(expected = RuntimeException.class)
    public void testGetHasResultWithThrowable() throws Exception {
        MessageFuture messageFuture = new MessageFuture();
        messageFuture.setRequestMessage(buildRepcMessage());
        messageFuture.setTimeout(TIME_OUT_FIELD);
        messageFuture.setResultMessage(new Throwable());
        messageFuture.get(TIME_OUT_FIELD, TimeUnit.MILLISECONDS);
    }

    /**
     * Test get has result with throwable with message.
     *
     * @throws Exception the exception
     */
    @Test
    public void testGetHasResultWithThrowableWithMessage() throws Exception {
        MessageFuture messageFuture = new MessageFuture();
        messageFuture.setRequestMessage(buildRepcMessage());
        messageFuture.setTimeout(TIME_OUT_FIELD);
        Throwable throwable = new Throwable("test_throwable");
        messageFuture.setResultMessage(throwable);
        try {
            messageFuture.get(TIME_OUT_FIELD, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            assertThat(e.getMessage()).isEqualTo(new RuntimeException(throwable).getMessage());
        }
    }

    private RpcMessage buildRepcMessage() {
        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setId(ID_FIELD);
        rpcMessage.setAsync(ASYNC_FIELD);
        rpcMessage.setRequest(REQUEST_FIELD);
        rpcMessage.setHeartbeat(HEART_BEAT_FIELD);
        rpcMessage.setBody(BODY_FIELD);
        return rpcMessage;
    }
}
