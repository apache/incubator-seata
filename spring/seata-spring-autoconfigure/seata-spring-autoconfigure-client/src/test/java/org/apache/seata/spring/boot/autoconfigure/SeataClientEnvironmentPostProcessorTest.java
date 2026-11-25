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
package org.apache.seata.spring.boot.autoconfigure;

import org.apache.seata.common.holder.ObjectHolder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;

import static org.apache.seata.common.Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT;
import static org.apache.seata.spring.boot.autoconfigure.StarterConstants.BOOTSTRAP_PROPERTY_SOURCE_NAME;

public class SeataClientEnvironmentPostProcessorTest {

    @Test
    public void testSeataClientEnvironmentPostProcessor() throws Exception {
        Object previousEnvironment = ObjectHolder.INSTANCE.getObject(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT);
        try (AnnotationConfigApplicationContext applicationContext =
                new AnnotationConfigApplicationContext("io.seata.spring.boot.autoconfigure.properties")) {
            SeataClientEnvironmentPostProcessor seataClientEnvironmentPostProcessor =
                    new SeataClientEnvironmentPostProcessor();
            seataClientEnvironmentPostProcessor.postProcessEnvironment(applicationContext.getEnvironment(), null);
            Assertions.assertEquals(Ordered.HIGHEST_PRECEDENCE, seataClientEnvironmentPostProcessor.getOrder());
            Assertions.assertSame(
                    applicationContext.getEnvironment(),
                    ObjectHolder.INSTANCE.getObject(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT));
        } finally {
            restoreEnvironment(previousEnvironment);
        }
    }

    @Test
    public void testSkipSpringCloudBootstrapEnvironment() throws Exception {
        Object previousEnvironment = ObjectHolder.INSTANCE.getObject(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT);
        try {
            removeEnvironmentFromObjectHolder();
            StandardEnvironment bootstrapEnvironment = new StandardEnvironment();
            bootstrapEnvironment
                    .getPropertySources()
                    .addFirst(new MapPropertySource(BOOTSTRAP_PROPERTY_SOURCE_NAME, Collections.emptyMap()));

            SeataClientEnvironmentPostProcessor processor = new SeataClientEnvironmentPostProcessor();
            processor.postProcessEnvironment(bootstrapEnvironment, null);
            Assertions.assertNull(ObjectHolder.INSTANCE.getObject(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT));

            StandardEnvironment mainEnvironment = new StandardEnvironment();
            processor.postProcessEnvironment(mainEnvironment, null);
            Assertions.assertSame(
                    mainEnvironment, ObjectHolder.INSTANCE.getObject(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT));
        } finally {
            restoreEnvironment(previousEnvironment);
        }
    }

    private static void restoreEnvironment(Object previousEnvironment) throws Exception {
        if (previousEnvironment == null) {
            removeEnvironmentFromObjectHolder();
        } else {
            ObjectHolder.INSTANCE.setObject(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT, previousEnvironment);
        }
    }

    @SuppressWarnings("unchecked")
    private static void removeEnvironmentFromObjectHolder() throws Exception {
        Field field = ObjectHolder.class.getDeclaredField("OBJECT_MAP");
        field.setAccessible(true);
        Map<String, Object> objectMap = (Map<String, Object>) field.get(null);
        objectMap.remove(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT);
    }
}
