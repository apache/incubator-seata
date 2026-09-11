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
package io.seata.spring.boot.autoconfigure;

import io.seata.spring.boot.autoconfigure.properties.SeataProperties;
import io.seata.spring.boot.autoconfigure.properties.client.LoadBalanceProperties;
import io.seata.spring.boot.autoconfigure.properties.client.LockProperties;
import io.seata.spring.boot.autoconfigure.properties.client.RmProperties;
import io.seata.spring.boot.autoconfigure.properties.client.ServiceProperties;
import io.seata.spring.boot.autoconfigure.properties.client.TmProperties;
import io.seata.spring.boot.autoconfigure.properties.client.UndoProperties;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.Map;

import static io.seata.common.DefaultValues.DEFAULT_GLOBAL_TRANSACTION_TIMEOUT;
import static io.seata.common.DefaultValues.DEFAULT_TM_COMMIT_RETRY_COUNT;
import static io.seata.common.DefaultValues.DEFAULT_TM_ROLLBACK_RETRY_COUNT;
import static io.seata.common.DefaultValues.DEFAULT_TRANSACTION_UNDO_LOG_TABLE;
import static io.seata.common.DefaultValues.DEFAULT_TX_GROUP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author xingfudeshi@gmail.com
 */
public class ClientPropertiesTest {
    private static AnnotationConfigApplicationContext context;

    @BeforeAll
    public static void initContext() {
        context = new AnnotationConfigApplicationContext("io.seata.spring.boot.autoconfigure.properties");
    }


    @Test
    public void testSeataProperties() {
        assertTrue(context.getBean(SeataProperties.class).isEnabled());
        assertNull(context.getBean(SeataProperties.class).getApplicationId());
        assertEquals(DEFAULT_TX_GROUP, context.getBean(SeataProperties.class).getTxServiceGroup());
        assertTrue(context.getBean(SeataProperties.class).isEnableAutoDataSourceProxy());
        assertEquals("AT", context.getBean(SeataProperties.class).getDataSourceProxyMode());
        assertFalse(context.getBean(SeataProperties.class).isUseJdkProxy());
    }


    @Test
    public void testLockProperties() {
        assertEquals(10, context.getBean(LockProperties.class).getRetryInterval());
        assertEquals(30, context.getBean(LockProperties.class).getRetryTimes());
        assertTrue(context.getBean(LockProperties.class).isRetryPolicyBranchRollbackOnConflict());
    }

    @Test
    public void testRmProperties() {
        assertEquals(10000, context.getBean(RmProperties.class).getAsyncCommitBufferLimit());
        assertEquals(5, context.getBean(RmProperties.class).getReportRetryCount());
        assertTrue(context.getBean(RmProperties.class).isTableMetaCheckEnable());
        assertFalse(context.getBean(RmProperties.class).isReportSuccessEnable());
        assertEquals(60000L,context.getBean(RmProperties.class).getTableMetaCheckerInterval());
        assertFalse(context.getBean(RmProperties.class).isSagaRetryPersistModeUpdate());
        assertFalse(context.getBean(RmProperties.class).isSagaCompensatePersistModeUpdate());
    }

    @Test
    public void testServiceProperties() {
        ServiceProperties serviceProperties = context.getBean(ServiceProperties.class);
        Map<String, String> vgroupMapping = serviceProperties.getVgroupMapping();
        Map<String, String> grouplist = serviceProperties.getGrouplist();
        assertEquals("default", vgroupMapping.get(DEFAULT_TX_GROUP));
        assertEquals("127.0.0.1:8091", grouplist.get("default"));
        assertFalse(serviceProperties.isEnableDegrade());
        assertFalse(serviceProperties.isDisableGlobalTransaction());
    }


    @Test
    public void testTmProperties() {
        assertEquals(DEFAULT_TM_COMMIT_RETRY_COUNT, context.getBean(TmProperties.class).getCommitRetryCount());
        assertEquals(DEFAULT_TM_ROLLBACK_RETRY_COUNT, context.getBean(TmProperties.class).getRollbackRetryCount());
        assertEquals(DEFAULT_GLOBAL_TRANSACTION_TIMEOUT, context.getBean(TmProperties.class).getDefaultGlobalTransactionTimeout());
    }

    @Test
    public void testUndoProperties() {
        assertTrue(context.getBean(UndoProperties.class).isDataValidation());
        assertEquals("jackson", context.getBean(UndoProperties.class).getLogSerialization());
        assertEquals(DEFAULT_TRANSACTION_UNDO_LOG_TABLE, context.getBean(UndoProperties.class).getLogTable());
    }

    @Test
    public void testLoadBalanceProperties() {
        assertEquals("XID", context.getBean(LoadBalanceProperties.class).getType());
        assertEquals(10, context.getBean(LoadBalanceProperties.class).getVirtualNodes());
    }


    @AfterAll
    public static void closeContext() {
        context.close();
    }
}
