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
package org.apache.seata.common.loader;

import org.junit.jupiter.api.Test;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The type Load level test.
 */
public class LoadLevelTest {

    @Test
    public void testLoadLevelAnnotation() {
        // Get the annotation from a class that uses it
        LoadLevel loadLevel = TestService.class.getAnnotation(LoadLevel.class);

        // Verify annotation values
        assertEquals("test", loadLevel.name());
        assertEquals(10, loadLevel.order());
        assertEquals(Scope.SINGLETON, loadLevel.scope());
    }

    @Test
    public void testLoadLevelDefaultValues() {
        LoadLevel loadLevel = DefaultService.class.getAnnotation(LoadLevel.class);

        // Verify default values
        assertEquals("default", loadLevel.name());
        assertEquals(0, loadLevel.order()); // Default value
        assertEquals(Scope.SINGLETON, loadLevel.scope()); // Default value
    }

    @Test
    public void testLoadLevelRetentionAndTarget() {
        // Test annotation properties
        Retention retention = LoadLevel.class.getAnnotation(Retention.class);
        assertTrue(retention != null);
        assertEquals(RetentionPolicy.RUNTIME, retention.value());

        Target target = LoadLevel.class.getAnnotation(Target.class);
        assertTrue(target != null);
        ElementType[] elementTypes = target.value();
        assertEquals(2, elementTypes.length);
        assertEquals(ElementType.TYPE, elementTypes[0]);
        assertEquals(ElementType.METHOD, elementTypes[1]);

        // Test that it's documented
        assertTrue(LoadLevel.class.isAnnotationPresent(Documented.class));
    }

    @LoadLevel(name = "test", order = 10, scope = Scope.SINGLETON)
    private static class TestService {
        // Test class for annotation testing
    }

    @LoadLevel(name = "default")
    private static class DefaultService {
        // Test class for default values testing
    }
}
