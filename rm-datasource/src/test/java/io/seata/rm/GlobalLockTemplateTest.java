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
package io.seata.rm;

import io.seata.core.context.GlobalLockConfigHolder;
import io.seata.core.context.RootContext;
import io.seata.core.model.GlobalLockConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author selfishlover
 */
public class GlobalLockTemplateTest {

    private final GlobalLockTemplate template = new GlobalLockTemplate();

    private final GlobalLockConfig config1 = generateGlobalLockConfig();

    private final GlobalLockConfig config2 = generateGlobalLockConfig();

    @BeforeEach
    void setUp() {
        RootContext.unbindGlobalLockFlag();
        GlobalLockConfigHolder.remove();
    }

    @Test
    void testSingle() {
        assertDoesNotThrow(() -> {
            template.execute(new GlobalLockExecutor() {
                @Override
                public Object execute() {
                    assertTrue(RootContext.requireGlobalLock(), "fail to bind global lock flag");
                    assertSame(config1, GlobalLockConfigHolder.getCurrentGlobalLockConfig(),
                            "global lock config changed during execution");
                    return null;
                }

                @Override
                public GlobalLockConfig getGlobalLockConfig() {
                    return config1;
                }
            });
        });
    }

    @Test
    void testNested() {
        assertDoesNotThrow(() -> {
            template.execute(new GlobalLockExecutor() {
                @Override
                public Object execute() {
                    assertTrue(RootContext.requireGlobalLock(), "fail to bind global lock flag");
                    assertSame(config1, GlobalLockConfigHolder.getCurrentGlobalLockConfig(),
                            "global lock config changed during execution");
                    assertDoesNotThrow(() -> {
                        template.execute(new GlobalLockExecutor() {
                            @Override
                            public Object execute() {
                                assertTrue(RootContext.requireGlobalLock(), "inner lost global lock flag");
                                assertSame(config2, GlobalLockConfigHolder.getCurrentGlobalLockConfig(),
                                        "fail to set inner global lock config");
                                return null;
                            }

                            @Override
                            public GlobalLockConfig getGlobalLockConfig() {
                                return config2;
                            }
                        });
                    });
                    assertTrue(RootContext.requireGlobalLock(), "outer lost global lock flag");
                    assertSame(config1, GlobalLockConfigHolder.getCurrentGlobalLockConfig(),
                            "outer global lock config was not restored");
                    return null;
                }

                @Override
                public GlobalLockConfig getGlobalLockConfig() {
                    return config1;
                }
            });
        });
    }

    @AfterEach
    void tearDown() {
        assertFalse(RootContext.requireGlobalLock(), "fail to unbind global lock flag");
        assertNull(GlobalLockConfigHolder.getCurrentGlobalLockConfig(), "fail to clean global lock config");
    }

    private GlobalLockConfig generateGlobalLockConfig() {
        GlobalLockConfig config = new GlobalLockConfig();
        config.setLockRetryInternal(100);
        config.setLockRetryTimes(3);
        return config;
    }
}