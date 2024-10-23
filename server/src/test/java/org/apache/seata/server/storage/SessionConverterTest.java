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
//package org.apache.seata.server.storage;
//
//import java.util.ArrayList;
//import java.util.ConcurrentModificationException;
//import java.util.List;
//import java.util.concurrent.CountDownLatch;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.concurrent.atomic.AtomicBoolean;
//import org.apache.seata.core.model.BranchStatus;
//import org.apache.seata.core.model.BranchType;
//import org.apache.seata.server.session.BranchSession;
//import org.junit.jupiter.api.RepeatedTest;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.boot.test.context.SpringBootTest;
//
//import static org.junit.jupiter.api.Assertions.assertFalse;
//
//@ExtendWith(MockitoExtension.class)
//@SpringBootTest
//public class SessionConverterTest {
//    // Repeat 100 for adding success per
//    @RepeatedTest(100)
//    public void testConcurrentModificationException() throws InterruptedException {
//        List<BranchSession> branchSessions = new ArrayList<>();
//        for (int i = 0; i < 1000; i++) {
//            branchSessions.add(createMockBranchSession(i));
//        }
//
//        CountDownLatch startLatch = new CountDownLatch(1);
//        CountDownLatch endLatch = new CountDownLatch(2);
//        AtomicBoolean exceptionThrown = new AtomicBoolean(false);
//
//        ExecutorService executorService = Executors.newFixedThreadPool(2);
//
//        // Thread for converting branch sessions
//        executorService.submit(() -> {
//            try {
//                startLatch.await();
//                for (int i = 0; i < 100; i++) {
//                    try {
//                        SessionConverter.convertBranchSession(branchSessions);
//                    } catch (ConcurrentModificationException e) {
//                        exceptionThrown.set(true);
//                        break;
//                    }
//                }
//            } catch (InterruptedException e) {
//                Thread.currentThread().interrupt();
//            } finally {
//                endLatch.countDown();
//            }
//        });
//
//        // Thread for modifying the list
//        executorService.submit(() -> {
//            try {
//                startLatch.await();
//                for (int i = 0; i < 1000; i++) {
//                    branchSessions.add(createMockBranchSession(1000 + i));
//                    if (i % 10 == 0) {
//                        branchSessions.remove(0);
//                    }
//                }
//            } catch (InterruptedException e) {
//                Thread.currentThread().interrupt();
//            } finally {
//                endLatch.countDown();
//            }
//        });
//        // Start both threads
//        startLatch.countDown();
//        // Wait for both threads to finish
//        endLatch.await();
//
//        executorService.shutdown();
//
//        assertFalse(exceptionThrown.get(), "ConcurrentModificationException was not thrown");
//    }
//
//    private BranchSession createMockBranchSession(int id) {
//        BranchSession session = new BranchSession();
//        session.setXid("xid" + id);
//        session.setTransactionId(id);
//        session.setBranchId(id);
//        session.setResourceGroupId("resourceGroup" + id);
//        session.setResourceId("resource" + id);
//        session.setBranchType(BranchType.AT);
//        session.setStatus(BranchStatus.Registered);
//        session.setClientId("client" + id);
//        session.setApplicationData("data" + id);
//        return session;
//    }
//}
