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
package io.seata.core.model;

import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.model.BranchStatus;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.core.model.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Unit test for ResourceManager interface
 */
public class ResourceManagerTest {

    /**
     * Mock implementation for testing
     */
    private static class MockResourceManager implements ResourceManager {
        private final Map<String, Resource> managedResources = new HashMap<>();

        @Override
        public Long branchRegister(
                BranchType branchType,
                String resourceId,
                String clientId,
                String xid,
                String applicationData,
                String lockKeys)
                throws TransactionException {
            return System.currentTimeMillis(); // Simple mock implementation
        }

        @Override
        public void branchReport(
                BranchType branchType, String xid, long branchId, BranchStatus status, String applicationData)
                throws TransactionException {
            // Mock implementation - do nothing
        }

        @Override
        public boolean lockQuery(BranchType branchType, String resourceId, String xid, String lockKeys)
                throws TransactionException {
            return true; // Simple mock implementation
        }

        @Override
        public BranchStatus branchCommit(
                BranchType branchType, String xid, long branchId, String resourceId, String applicationData)
                throws TransactionException {
            return BranchStatus.PhaseTwo_Committed; // Simple mock implementation
        }

        @Override
        public BranchStatus branchRollback(
                BranchType branchType, String xid, long branchId, String resourceId, String applicationData)
                throws TransactionException {
            return BranchStatus.PhaseTwo_Rollbacked; // Simple mock implementation
        }

        @Override
        public void registerResource(Resource resource) {
            managedResources.put(resource.getResourceId(), resource);
        }

        @Override
        public void unregisterResource(Resource resource) {
            managedResources.remove(resource.getResourceId());
        }

        @Override
        public Map<String, Resource> getManagedResources() {
            return new HashMap<>(managedResources);
        }

        @Override
        public BranchType getBranchType() {
            return BranchType.AT; // Simple mock implementation
        }

        @Override
        public GlobalStatus getGlobalStatus(BranchType branchType, String xid) {
            return GlobalStatus.Begin; // Simple mock implementation
        }
    }

    @Test
    public void testResourceManagerInterfaceInheritance() {
        // Test that ResourceManager extends from Apache Seata's ResourceManager
        Assertions.assertTrue(
                org.apache.seata.core.model.ResourceManager.class.isAssignableFrom(ResourceManager.class));
    }

    @Test
    public void testDeprecationAnnotation() {
        // Test that the ResourceManager interface is marked as deprecated
        Assertions.assertTrue(
                ResourceManager.class.isAnnotationPresent(Deprecated.class),
                "ResourceManager should be marked as @Deprecated");
    }

    @Test
    public void testInterfaceStructure() {
        // Test interface modifiers
        int modifiers = ResourceManager.class.getModifiers();
        Assertions.assertTrue(
                java.lang.reflect.Modifier.isInterface(modifiers), "ResourceManager should be an interface");
        Assertions.assertTrue(java.lang.reflect.Modifier.isPublic(modifiers), "ResourceManager should be public");
    }

    @Test
    public void testPackageName() {
        // Test that the package is the expected compatible package
        Assertions.assertEquals(
                "io.seata.core.model",
                ResourceManager.class.getPackage().getName(),
                "ResourceManager should be in io.seata.core.model package");
    }

    @Test
    public void testMethodInheritance() throws Exception {
        // Test that the interface has the expected methods from the parent interface
        MockResourceManager mockResourceManager = new MockResourceManager();

        // Test that the mock resource manager implements both interfaces
        Assertions.assertTrue(mockResourceManager instanceof ResourceManager);
        Assertions.assertTrue(mockResourceManager instanceof org.apache.seata.core.model.ResourceManager);
    }

    @Test
    public void testInterfaceCompatibility() {
        // Test that compatible ResourceManager can be used wherever Apache Seata ResourceManager is expected
        MockResourceManager compatibleResourceManager = new MockResourceManager();
        org.apache.seata.core.model.ResourceManager apacheResourceManager = compatibleResourceManager;

        Assertions.assertNotNull(apacheResourceManager);
        Assertions.assertSame(compatibleResourceManager, apacheResourceManager);
    }

    @Test
    public void testImplementationFunctionality() throws TransactionException {
        // Test basic functionality of a mock implementation
        MockResourceManager resourceManager = new MockResourceManager();

        // Test branchRegister method
        Long branchId = resourceManager.branchRegister(BranchType.AT, "resource1", "client1", "xid1", "data", "keys");
        Assertions.assertNotNull(branchId);
        Assertions.assertTrue(branchId > 0);

        // Test lockQuery method
        boolean lockResult = resourceManager.lockQuery(BranchType.AT, "resource1", "xid1", "keys");
        Assertions.assertTrue(lockResult);

        // Test branchCommit method
        BranchStatus commitStatus = resourceManager.branchCommit(BranchType.AT, "xid1", branchId, "resource1", "data");
        Assertions.assertEquals(BranchStatus.PhaseTwo_Committed, commitStatus);

        // Test branchRollback method
        BranchStatus rollbackStatus =
                resourceManager.branchRollback(BranchType.AT, "xid1", branchId, "resource1", "data");
        Assertions.assertEquals(BranchStatus.PhaseTwo_Rollbacked, rollbackStatus);

        // Test getBranchType method
        BranchType branchType = resourceManager.getBranchType();
        Assertions.assertEquals(BranchType.AT, branchType);

        // Test getGlobalStatus method
        GlobalStatus globalStatus = resourceManager.getGlobalStatus(BranchType.AT, "xid1");
        Assertions.assertEquals(GlobalStatus.Begin, globalStatus);
    }

    @Test
    public void testResourceManagement() {
        // Test resource registration and management
        MockResourceManager resourceManager = new MockResourceManager();

        // Create a mock resource
        Resource mockResource = new Resource() {
            @Override
            public String getResourceGroupId() {
                return "testGroup";
            }

            @Override
            public String getResourceId() {
                return "testResource";
            }

            @Override
            public BranchType getBranchType() {
                return BranchType.AT;
            }
        };

        // Test registerResource method
        resourceManager.registerResource(mockResource);
        Map<String, Resource> managedResources = resourceManager.getManagedResources();
        Assertions.assertTrue(managedResources.containsKey("testResource"));
        Assertions.assertEquals(mockResource, managedResources.get("testResource"));

        // Test unregisterResource method
        resourceManager.unregisterResource(mockResource);
        managedResources = resourceManager.getManagedResources();
        Assertions.assertFalse(managedResources.containsKey("testResource"));
    }

    @Test
    public void testPolymorphism() {
        // Test polymorphic behavior
        MockResourceManager mockResourceManager = new MockResourceManager();
        ResourceManager resourceManager = mockResourceManager;
        Object obj = resourceManager;

        Assertions.assertTrue(obj instanceof org.apache.seata.core.model.ResourceManager);
        Assertions.assertTrue(obj instanceof ResourceManager);
    }

    @Test
    public void testRequiredMethods() throws Exception {
        // Test that all required methods are present
        java.lang.reflect.Method[] methods = ResourceManager.class.getMethods();

        boolean hasBranchRegister = false;
        boolean hasBranchReport = false;
        boolean hasLockQuery = false;
        boolean hasBranchCommit = false;
        boolean hasBranchRollback = false;
        boolean hasRegisterResource = false;
        boolean hasUnregisterResource = false;
        boolean hasGetManagedResources = false;
        boolean hasGetBranchType = false;
        boolean hasGetGlobalStatus = false;

        for (java.lang.reflect.Method method : methods) {
            switch (method.getName()) {
                case "branchRegister":
                    if (method.getParameterCount() == 6) hasBranchRegister = true;
                    break;
                case "branchReport":
                    if (method.getParameterCount() == 5) hasBranchReport = true;
                    break;
                case "lockQuery":
                    if (method.getParameterCount() == 4) hasLockQuery = true;
                    break;
                case "branchCommit":
                    if (method.getParameterCount() == 5) hasBranchCommit = true;
                    break;
                case "branchRollback":
                    if (method.getParameterCount() == 5) hasBranchRollback = true;
                    break;
                case "registerResource":
                    if (method.getParameterCount() == 1) hasRegisterResource = true;
                    break;
                case "unregisterResource":
                    if (method.getParameterCount() == 1) hasUnregisterResource = true;
                    break;
                case "getManagedResources":
                    if (method.getParameterCount() == 0) hasGetManagedResources = true;
                    break;
                case "getBranchType":
                    if (method.getParameterCount() == 0) hasGetBranchType = true;
                    break;
                case "getGlobalStatus":
                    if (method.getParameterCount() == 2) hasGetGlobalStatus = true;
                    break;
            }
        }

        Assertions.assertTrue(hasBranchRegister, "ResourceManager should have branchRegister method");
        Assertions.assertTrue(hasBranchReport, "ResourceManager should have branchReport method");
        Assertions.assertTrue(hasLockQuery, "ResourceManager should have lockQuery method");
        Assertions.assertTrue(hasBranchCommit, "ResourceManager should have branchCommit method");
        Assertions.assertTrue(hasBranchRollback, "ResourceManager should have branchRollback method");
        Assertions.assertTrue(hasRegisterResource, "ResourceManager should have registerResource method");
        Assertions.assertTrue(hasUnregisterResource, "ResourceManager should have unregisterResource method");
        Assertions.assertTrue(hasGetManagedResources, "ResourceManager should have getManagedResources method");
        Assertions.assertTrue(hasGetBranchType, "ResourceManager should have getBranchType method");
        Assertions.assertTrue(hasGetGlobalStatus, "ResourceManager should have getGlobalStatus method");
    }
}
