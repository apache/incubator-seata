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
package org.apache.seata.server.lock;

import org.apache.seata.common.Constants;
import org.apache.seata.common.holder.ObjectHolder;
import org.apache.seata.core.rpc.RemotingServer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.springframework.mock.env.MockEnvironment;

import javax.tools.ToolProvider;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class LockManagerBinaryCompatibilityTest {
    @TempDir
    Path tempDir;

    @Test
    @SuppressWarnings("unchecked")
    void testOldCompiledCallersLinkAndResolveReplacementManagers() throws Exception {
        // Compile against the old API descriptors, then link only the callers against current classes.
        Path stubs = tempDir.resolve("stubs");
        Path callers = tempDir.resolve("callers");
        Files.createDirectories(stubs);
        Files.createDirectories(callers);
        List<String> sources = new ArrayList<>();
        sources.add(
                source(
                        "org/apache/seata/server/lock/LockerManagerFactory.java",
                        "package org.apache.seata.server.lock; public class LockerManagerFactory {"
                                + " public static void init() {} public static void init(org.apache.seata.common.store.LockMode mode) {} }"));
        sources.add(
                source(
                        "org/apache/seata/server/storage/file/lock/FileLockManager.java",
                        "package org.apache.seata.server.storage.file.lock; public class FileLockManager { public FileLockManager() {} }"));
        sources.add(source(
                "org/apache/seata/server/console/impl/AbstractService.java",
                "package org.apache.seata.server.console.impl; public abstract class AbstractService {"
                        + " protected final org.apache.seata.server.lock.LockManager lockManager = null; }"));
        sources.add(source(
                "org/apache/seata/server/coordinator/AbstractCore.java",
                "package org.apache.seata.server.coordinator; public abstract class AbstractCore {"
                        + " protected org.apache.seata.server.lock.LockManager lockManager; }"));
        sources.add(source(
                "org/apache/seata/server/transaction/at/ATCore.java",
                "package org.apache.seata.server.transaction.at; public class ATCore extends org.apache.seata.server.coordinator.AbstractCore {"
                        + " public ATCore(org.apache.seata.core.rpc.RemotingServer server) {} }"));
        compile(stubs, System.getProperty("java.class.path"), sources);
        sources.clear();
        sources.add(source(
                "legacy/ServiceCaller.java",
                "package legacy; public class ServiceCaller extends org.apache.seata.server.console.impl.AbstractService {"
                        + " public org.apache.seata.server.lock.LockManager manager() { return lockManager; }"
                        + " public void initialize() { org.apache.seata.server.lock.LockerManagerFactory.init();"
                        + " org.apache.seata.server.lock.LockerManagerFactory.init(org.apache.seata.common.store.LockMode.FILE);"
                        + " new org.apache.seata.server.storage.file.lock.FileLockManager(); } }"));
        sources.add(
                source(
                        "legacy/CoreCaller.java",
                        "package legacy; public class CoreCaller extends org.apache.seata.server.transaction.at.ATCore {"
                                + " public CoreCaller(org.apache.seata.core.rpc.RemotingServer server) { super(server); }"
                                + " public org.apache.seata.server.lock.LockManager manager() { return lockManager; }"
                                + " public void replace(org.apache.seata.server.lock.LockManager replacement) { lockManager = replacement; } }"));
        compile(callers, stubs + java.io.File.pathSeparator + System.getProperty("java.class.path"), sources);
        Object environment = ObjectHolder.INSTANCE.getObject(Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT);
        ObjectHolder.INSTANCE.setObject(Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT, new MockEnvironment());
        Field managerField = LockerManagerFactory.class.getDeclaredField("LOCK_MANAGER");
        managerField.setAccessible(true);
        Object originalManager = managerField.get(null);
        try (URLClassLoader loader = new URLClassLoader(
                new URL[] {callers.toUri().toURL()}, getClass().getClassLoader())) {
            managerField.set(null, null);
            Class<?> serviceType = loader.loadClass("legacy.ServiceCaller");
            Object service = serviceType.getConstructor().newInstance();
            Class<?> coreType = loader.loadClass("legacy.CoreCaller");
            Object core = coreType.getConstructor(RemotingServer.class).newInstance(Mockito.mock(RemotingServer.class));
            Assertions.assertNull(managerField.get(null));
            LockManager first = Mockito.mock(LockManager.class);
            managerField.set(null, first);
            serviceType.getMethod("initialize").invoke(service);
            LockManager serviceFacade =
                    (LockManager) serviceType.getMethod("manager").invoke(service);
            LockManager coreFacade = (LockManager) coreType.getMethod("manager").invoke(core);
            serviceFacade.cleanAllLocks();
            coreFacade.cleanAllLocks();
            Mockito.verify(first, Mockito.times(2)).cleanAllLocks();
            LockManager replacement = Mockito.mock(LockManager.class);
            managerField.set(null, replacement);
            serviceFacade.cleanAllLocks();
            coreFacade.cleanAllLocks();
            Mockito.verify(replacement, Mockito.times(2)).cleanAllLocks();
            coreType.getMethod("replace", LockManager.class).invoke(core, first);
            Assertions.assertSame(first, coreType.getMethod("manager").invoke(core));
        } finally {
            managerField.set(null, originalManager);
            Field objects = ObjectHolder.class.getDeclaredField("OBJECT_MAP");
            objects.setAccessible(true);
            Map<String, Object> values = (Map<String, Object>) objects.get(ObjectHolder.INSTANCE);
            if (environment == null) {
                values.remove(Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT);
            } else {
                values.put(Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT, environment);
            }
        }
    }

    private String source(String name, String content) throws Exception {
        Path path = tempDir.resolve("src").resolve(name);
        Files.createDirectories(path.getParent());
        Files.write(path, content.getBytes(StandardCharsets.UTF_8));
        return path.toString();
    }

    private void compile(Path output, String classpath, List<String> sources) {
        List<String> args = new ArrayList<>();
        java.util.Collections.addAll(args, "-classpath", classpath, "-d", output.toString());
        args.addAll(sources);
        Assertions.assertEquals(
                0, ToolProvider.getSystemJavaCompiler().run(null, null, null, args.toArray(new String[0])));
    }
}
