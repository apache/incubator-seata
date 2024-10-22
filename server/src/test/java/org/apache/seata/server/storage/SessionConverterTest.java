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
//import static org.junit.jupiter.api.Assertions.assertTrue;
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
//        assertTrue(exceptionThrown.get(), "ConcurrentModificationException was not thrown");
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
