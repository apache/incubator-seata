package org.apache.seata.spring.rm.fence;

import org.apache.seata.common.exception.FrameworkErrorCode;
import org.apache.seata.common.exception.SkipCallbackWrapperException;
import org.apache.seata.common.executor.Callback;
import org.apache.seata.integration.tx.api.fence.constant.CommonFenceConstant;
import org.apache.seata.integration.tx.api.fence.exception.CommonFenceException;
import org.apache.seata.integration.tx.api.fence.store.CommonFenceDO;
import org.apache.seata.integration.tx.api.fence.store.CommonFenceStore;
import org.apache.seata.rm.fence.SpringFenceHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class SpringFenceHandlerTest {

    private DataSource dataSource;
    private Connection connection;
    private CommonFenceStore fenceStore;
    private TransactionTemplate transactionTemplate;

    private final SpringFenceHandler handler = new SpringFenceHandler();

    @BeforeEach
    void setUp() throws Exception {
        dataSource = mock(DataSource.class);
        connection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(connection);
        SpringFenceHandler.setDataSource(dataSource);

        PlatformTransactionManager txManager = mock(PlatformTransactionManager.class);
        transactionTemplate = spy(new TransactionTemplate(txManager));
        doAnswer(invocation -> {
                    TransactionCallback<?> callback = invocation.getArgument(0);
                    return callback.doInTransaction(mock(TransactionStatus.class));
                })
                .when(transactionTemplate)
                .execute(any());

        SpringFenceHandler.setTransactionTemplate(transactionTemplate);

        fenceStore = mock(CommonFenceStore.class);
        setStaticFinalField(SpringFenceHandler.class, "COMMON_FENCE_DAO", fenceStore);
    }

    @Test
    void testPrepareFence_success() throws Throwable {
        when(fenceStore.insertCommonFenceDO(any(), any())).thenReturn(true);

        Callback<Object> callback = mock(Callback.class);
        when(callback.execute()).thenReturn("OK");

        Object result = handler.prepareFence("xid123", 1L, "action", callback);
        assertEquals("OK", result);
    }

    @Test
    void testPrepareFence_duplicateKey() {
        when(fenceStore.insertCommonFenceDO(any(), any()))
                .thenThrow(new CommonFenceException(FrameworkErrorCode.DuplicateKeyException));

        assertThrows(
                SkipCallbackWrapperException.class,
                () -> handler.prepareFence("xid", 1L, "action", mock(Callback.class)));
    }

    @Test
    void testCommitFence_success() throws Exception {
        CommonFenceDO fenceDO = new CommonFenceDO();
        fenceDO.setStatus(CommonFenceConstant.STATUS_TRIED);

        when(fenceStore.queryCommonFenceDO(any(), eq("xid"), eq(100L))).thenReturn(fenceDO);
        when(fenceStore.updateCommonFenceDO(
                        any(),
                        eq("xid"),
                        eq(100L),
                        eq(CommonFenceConstant.STATUS_COMMITTED),
                        eq(CommonFenceConstant.STATUS_TRIED)))
                .thenReturn(true);

        Method method = TestTCC.class.getMethod("commitMethod");
        boolean result = handler.commitFence(method, new TestTCC(), "xid", 100L, new Object[0]);
        assertTrue(result);
    }

    @Test
    void testCommitFence_alreadyCommitted() throws Exception {
        CommonFenceDO fenceDO = new CommonFenceDO();
        fenceDO.setStatus(CommonFenceConstant.STATUS_COMMITTED);

        when(fenceStore.queryCommonFenceDO(any(), anyString(), anyLong())).thenReturn(fenceDO);

        Method method = TestTCC.class.getMethod("commitMethod");
        boolean result = handler.commitFence(method, new TestTCC(), "xid", 200L, new Object[0]);
        assertTrue(result);
    }

    @Test
    void testRollbackFence_success() throws Exception {
        CommonFenceDO fenceDO = new CommonFenceDO();
        fenceDO.setStatus(CommonFenceConstant.STATUS_TRIED);

        when(fenceStore.queryCommonFenceDO(any(), anyString(), anyLong())).thenReturn(fenceDO);
        when(fenceStore.updateCommonFenceDO(
                        any(),
                        anyString(),
                        anyLong(),
                        eq(CommonFenceConstant.STATUS_ROLLBACKED),
                        eq(CommonFenceConstant.STATUS_TRIED)))
                .thenReturn(true);

        Method method = TestTCC.class.getMethod("rollbackMethod");
        boolean result = handler.rollbackFence(method, new TestTCC(), "xid", 300L, new Object[0], "action");
        assertTrue(result);
    }

    @Test
    void testRollbackFence_insertSuspended() throws Exception {
        // 模拟 record 不存在
        when(fenceStore.queryCommonFenceDO(any(), anyString(), anyLong())).thenReturn(null);
        when(fenceStore.insertCommonFenceDO(any(), any())).thenReturn(true);

        Method method = TestTCC.class.getMethod("rollbackMethod");
        boolean result = handler.rollbackFence(method, new TestTCC(), "xid", 400L, new Object[0], "action");
        assertTrue(result);
    }

    @Test
    void testRollbackFence_alreadyRollbacked() throws Exception {
        CommonFenceDO fenceDO = new CommonFenceDO();
        fenceDO.setStatus(CommonFenceConstant.STATUS_ROLLBACKED);

        when(fenceStore.queryCommonFenceDO(any(), anyString(), anyLong())).thenReturn(fenceDO);

        Method method = TestTCC.class.getMethod("rollbackMethod");
        boolean result = handler.rollbackFence(method, new TestTCC(), "xid", 500L, new Object[0], "action");
        assertTrue(result);
    }

    @Test
    void testDeleteFenceByDate() throws Exception {
        Date now = new Date();
        Set<String> xidSet = new HashSet<>();
        xidSet.add("xid-1");
        xidSet.add("xid-2");

        // 第一次查出一批 xids
        when(fenceStore.queryEndStatusXidsByDate(any(), eq(now), anyInt()))
                .thenReturn(xidSet)
                // 第二次查空表示结束
                .thenReturn(Collections.emptySet());

        when(fenceStore.deleteTCCFenceDO(any(), anyList())).thenReturn(xidSet.size());

        int deleted = handler.deleteFenceByDate(now);
        assertEquals(xidSet.size(), deleted);
    }

    /*@Test
    void testFenceLogCleanRunnableDeletesSuccessfully() throws Exception {
        // 构造待清理的 xid 和 branchId
        String xid = "xid-clean";
        long branchId = 100L;

        Class<?> fenceLogIdentityClass = Class.forName("org.apache.seata.rm.fence.SpringFenceHandler$FenceLogIdentity");

        Constructor<?> constructor = fenceLogIdentityClass.getDeclaredConstructor();
        constructor.setAccessible(true);
        Object logIdentity = constructor.newInstance();

        Method setXidMethod = fenceLogIdentityClass.getDeclaredMethod("setXid", String.class);
        setXidMethod.setAccessible(true);
        setXidMethod.invoke(logIdentity, xid);

        Method setBranchIdMethod = fenceLogIdentityClass.getDeclaredMethod("setBranchId", Long.class);
        setBranchIdMethod.setAccessible(true);
        setBranchIdMethod.invoke(logIdentity, branchId);

        Field queueField = SpringFenceHandler.class.getDeclaredField("LOG_QUEUE");
        queueField.setAccessible(true);
        LinkedBlockingQueue<Object> queue = (LinkedBlockingQueue<Object>) queueField.get(null);
        queue.clear();
        queue.add(logIdentity);

        // mock 静态方法 deleteFence 返回 true
        try (MockedStatic<SpringFenceHandler> mockedStatic = mockStatic(SpringFenceHandler.class)) {
            mockedStatic.when(() -> SpringFenceHandler.deleteFence(eq(xid), anyLong())).thenReturn(true);

            // 获取 FenceLogCleanRunnable 的类
            Class<?> fenceLogCleanRunnableClass = null;
            for (Class<?> innerClass : SpringFenceHandler.class.getDeclaredClasses()) {
                if ("FenceLogCleanRunnable".equals(innerClass.getSimpleName())) {
                    fenceLogCleanRunnableClass = innerClass;
                    break;
                }
            }
            assertNotNull(fenceLogCleanRunnableClass, "FenceLogCleanRunnable class not found");

            // 实例化 Runnable
            Constructor<?> cleanRunnableConstructor = fenceLogCleanRunnableClass.getDeclaredConstructor();
            cleanRunnableConstructor.setAccessible(true);
            Runnable cleanRunnable = (Runnable) cleanRunnableConstructor.newInstance();

            // 直接在当前线程调用 run()，确保静态 mock 生效
            cleanRunnable.run();

            // 验证静态方法调用次数
            mockedStatic.verify(() -> SpringFenceHandler.deleteFence(xid, branchId), times(1));
        }
    }*/

    /**
     * 反射设置静态 final 字段（绕过 COMMON_FENCE_DAO 访问限制）
     */
    static void setStaticFinalField(Class<?> clazz, String fieldName, Object value) throws Exception {
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);

        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~java.lang.reflect.Modifier.FINAL);

        field.set(null, value);
    }

    /**
     * 模拟 TCC bean
     */
    public static class TestTCC {
        public boolean commitMethod() {
            return true;
        }

        public boolean rollbackMethod() {
            return true;
        }
    }
}
