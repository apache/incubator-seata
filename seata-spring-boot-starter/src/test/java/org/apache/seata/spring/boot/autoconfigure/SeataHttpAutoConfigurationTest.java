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

import org.apache.seata.integration.http.JakartaSeataWebMvcConfigurer;
import org.apache.seata.integration.http.SeataWebMvcConfigurer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import static org.apache.seata.spring.boot.autoconfigure.StarterConstants.HTTP_PREFIX;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SeataHttpAutoConfiguration} to verify conditional bean registration.
 */
public class SeataHttpAutoConfigurationTest {
    // No thread safety issues, no need to create in @BeforeEach
    private final WebApplicationContextRunner webContextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SeataHttpAutoConfiguration.class));
    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(SeataHttpAutoConfiguration.class));

    @Test
    void whenNotWebApplication_thenNoBeansCreated() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(SeataWebMvcConfigurer.class);
            assertThat(context).doesNotHaveBean(JakartaSeataWebMvcConfigurer.class);
        });
    }

    @Test
    void whenInterceptorDisabled_thenNoBeansCreated() {
        webContextRunner
                .withPropertyValues(HTTP_PREFIX + ".interceptor-enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(SeataWebMvcConfigurer.class);
                    assertThat(context).doesNotHaveBean(JakartaSeataWebMvcConfigurer.class);
                });
    }

    @Test
    void whenJakartaClassMissing_thenCreatesSeataWebMvcConfigurer() {
        webContextRunner
                .withClassLoader(new FilteredClassLoader("jakarta.servlet.http.HttpServletRequest"))
                .run(context -> {
                    assertThat(context).hasSingleBean(SeataWebMvcConfigurer.class);
                    assertThat(context).doesNotHaveBean(JakartaSeataWebMvcConfigurer.class);
                });
    }

    @Test
    void whenJakartaClassPresent_thenCreatesJakartaSeataWebMvcConfigurer() {
        webContextRunner.run(context -> {
            // Do not use assertThat(context).doesNotHaveBean(SeataWebMvcConfigurer.class),
            // because JakartaSeataWebMvcConfigurer extends SeataWebMvcConfigurer.
            assertThat(context.getBeansOfType(SeataWebMvcConfigurer.class).values())
                    .hasOnlyElementsOfType(JakartaSeataWebMvcConfigurer.class);
        });
    }
}
