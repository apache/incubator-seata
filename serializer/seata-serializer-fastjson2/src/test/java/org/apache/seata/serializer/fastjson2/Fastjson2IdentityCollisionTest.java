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
package org.apache.seata.serializer.fastjson2;

import com.alibaba.fastjson2.writer.ObjectWriterProvider;
import org.apache.seata.core.protocol.BatchResultMessage;
import org.apache.seata.core.protocol.MergedWarpMessage;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class Fastjson2IdentityCollisionTest {

    @Test
    public void serializationPreservesReferencesWhenClassIdentityHashesCollide() throws Exception {
        Process process = new ProcessBuilder(
                        System.getProperty("java.home") + "/bin/java",
                        "-cp",
                        System.getProperty("java.class.path"),
                        Fastjson2IdentityCollisionTest.class.getName())
                .redirectErrorStream(true)
                .start();
        if (!process.waitFor(30, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new AssertionError("Timed out waiting for the collision test process");
        }
        assertEquals(0, process.exitValue(), () -> "Child process failed: " + readOutput(process.getInputStream()));
    }

    public static void main(String[] args) throws Exception {
        // Simulate a hash collision only in this child JVM; never modify fastjson2 state in the test runner.
        Field hashesField = ObjectWriterProvider.class.getDeclaredField("NOT_REFERENCES_TYPE_HASH_CODES");
        hashesField.setAccessible(true);
        int[] hashes = (int[]) hashesField.get(null);
        hashes[0] = System.identityHashCode(BatchResultMessage.class);
        Arrays.sort(hashes);

        Fastjson2Serializer serializer = new Fastjson2Serializer();
        serializer.init();
        MergedWarpMessage input = referenceHeavyMessage();
        MergedWarpMessage decoded = serializer.deserialize(serializer.serialize(input));

        assertEquals(2, decoded.msgs.size());
        BatchResultMessage first = (BatchResultMessage) decoded.msgs.get(0);
        BatchResultMessage second = (BatchResultMessage) decoded.msgs.get(1);
        Object shared = first.getMsgIds();
        assertTrue(shared != null && shared == first.getResultMessages(), "first shared reference was lost");
        assertTrue(shared == second.getMsgIds() && shared == second.getResultMessages(), "shared references were lost");
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static MergedWarpMessage referenceHeavyMessage() {
        MergedWarpMessage message = new MergedWarpMessage();
        List shared = new ArrayList();
        for (int i = 0; i < 2; i++) {
            BatchResultMessage child = new BatchResultMessage();
            child.setMsgIds(shared);
            child.setResultMessages(shared);
            message.msgs.add(child);
            message.msgIds.add(i);
        }
        return message;
    }

    private static String readOutput(InputStream input) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = input.read(buffer)) != -1) {
                output.write(buffer, 0, length);
            }
            return output.toString("UTF-8");
        } catch (Exception e) {
            throw new AssertionError("Could not read child process output", e);
        }
    }
}
