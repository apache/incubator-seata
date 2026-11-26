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
package org.apache.seata.core.store;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Unit tests for default methods on {@link LogStore}.
 */
public class LogStoreTest {

    @Test
    public void testDeleteGlobalTransactionDOsDefaultFallback() {
        List<String> deletedXids = new ArrayList<>();
        LogStore logStore = new StubLogStore(deletedXids, null);

        Assertions.assertTrue(logStore.deleteGlobalTransactionDOs(null));
        Assertions.assertTrue(logStore.deleteGlobalTransactionDOs(Collections.emptyList()));
        Assertions.assertTrue(deletedXids.isEmpty());

        GlobalTransactionDO first = new GlobalTransactionDO("xid-1");
        GlobalTransactionDO second = new GlobalTransactionDO("xid-2");
        Assertions.assertTrue(logStore.deleteGlobalTransactionDOs(Arrays.asList(first, null, second)));
        Assertions.assertEquals(Arrays.asList("xid-1", "xid-2"), deletedXids);
    }

    @Test
    public void testDeleteGlobalTransactionDOsShortCircuitsOnFailure() {
        List<String> deletedXids = new ArrayList<>();
        LogStore logStore = new StubLogStore(deletedXids, "xid-2");

        GlobalTransactionDO first = new GlobalTransactionDO("xid-1");
        GlobalTransactionDO second = new GlobalTransactionDO("xid-2");
        GlobalTransactionDO third = new GlobalTransactionDO("xid-3");
        Assertions.assertFalse(logStore.deleteGlobalTransactionDOs(Arrays.asList(first, second, third)));
        Assertions.assertEquals(Arrays.asList("xid-1", "xid-2"), deletedXids);
    }

    private static class StubLogStore implements LogStore {
        private final List<String> deletedXids;
        private final String failOnXid;

        StubLogStore(List<String> deletedXids, String failOnXid) {
            this.deletedXids = deletedXids;
            this.failOnXid = failOnXid;
        }

        @Override
        public GlobalTransactionDO queryGlobalTransactionDO(String xid) {
            return null;
        }

        @Override
        public GlobalTransactionDO queryGlobalTransactionDO(long transactionId) {
            return null;
        }

        @Override
        public List<GlobalTransactionDO> queryGlobalTransactionDO(int[] status, int limit) {
            return Collections.emptyList();
        }

        @Override
        public boolean insertGlobalTransactionDO(GlobalTransactionDO globalTransactionDO) {
            return false;
        }

        @Override
        public boolean updateGlobalTransactionDO(GlobalTransactionDO globalTransactionDO) {
            return false;
        }

        @Override
        public boolean updateGlobalTransactionDO(GlobalTransactionDO globalTransactionDO, Integer expectedStatus) {
            return false;
        }

        @Override
        public boolean deleteGlobalTransactionDO(GlobalTransactionDO globalTransactionDO) {
            deletedXids.add(globalTransactionDO.getXid());
            return !globalTransactionDO.getXid().equals(failOnXid);
        }

        @Override
        public List<BranchTransactionDO> queryBranchTransactionDO(String xid) {
            return Collections.emptyList();
        }

        @Override
        public List<BranchTransactionDO> queryBranchTransactionDO(List<String> xids) {
            return Collections.emptyList();
        }

        @Override
        public boolean insertBranchTransactionDO(BranchTransactionDO branchTransactionDO) {
            return false;
        }

        @Override
        public boolean updateBranchTransactionDO(BranchTransactionDO branchTransactionDO) {
            return false;
        }

        @Override
        public boolean deleteBranchTransactionDO(BranchTransactionDO branchTransactionDO) {
            return false;
        }

        @Override
        public long getCurrentMaxSessionId(long high, long low) {
            return 0;
        }
    }
}
