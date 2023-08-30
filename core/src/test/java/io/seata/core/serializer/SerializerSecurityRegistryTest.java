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

package io.seata.core.serializer;

import io.seata.core.protocol.HeartbeatMessage;
import io.seata.core.protocol.Version;
import io.seata.core.protocol.transaction.AbstractBranchEndRequest;
import io.seata.core.protocol.transaction.BranchCommitRequest;
import io.seata.core.protocol.transaction.BranchCommitResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author slievrly
 */
public class SerializerSecurityRegistryTest {
    @Test
    public void getAllowClassType() {
        Assertions.assertTrue(SerializerSecurityRegistry.getAllowClassType().contains(Long.class));
        Assertions.assertTrue(SerializerSecurityRegistry.getAllowClassType().contains(Integer.class));
        Assertions.assertTrue(SerializerSecurityRegistry.getAllowClassType().contains(HeartbeatMessage.class));
        Assertions.assertTrue(SerializerSecurityRegistry.getAllowClassType().contains(BranchCommitRequest.class));
        Assertions.assertTrue(SerializerSecurityRegistry.getAllowClassType().contains(BranchCommitResponse.class));
        Assertions.assertFalse(SerializerSecurityRegistry.getAllowClassType().contains(AbstractBranchEndRequest.class));
        Assertions.assertFalse(SerializerSecurityRegistry.getAllowClassType().contains(Version.class));
    }

    @Test
    public void getAllowClassPattern() {
        Assertions.assertTrue(
            SerializerSecurityRegistry.getAllowClassPattern().contains(Long.class.getCanonicalName()));
        Assertions.assertTrue(
            SerializerSecurityRegistry.getAllowClassPattern().contains(Integer.class.getCanonicalName()));
        Assertions.assertTrue(
            SerializerSecurityRegistry.getAllowClassPattern().contains(HeartbeatMessage.class.getCanonicalName()));
        Assertions.assertTrue(
            SerializerSecurityRegistry.getAllowClassPattern().contains(BranchCommitRequest.class.getCanonicalName()));
        Assertions.assertTrue(
            SerializerSecurityRegistry.getAllowClassPattern().contains(BranchCommitResponse.class.getCanonicalName()));
        Assertions.assertFalse(SerializerSecurityRegistry.getAllowClassPattern()
            .contains(AbstractBranchEndRequest.class.getCanonicalName()));
        Assertions.assertFalse(
            SerializerSecurityRegistry.getAllowClassPattern().contains(Version.class.getCanonicalName()));
        Assertions.assertTrue(SerializerSecurityRegistry.getAllowClassPattern().contains("io.seata.*"));
    }
}
