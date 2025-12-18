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

import io.seata.tm.api.DefaultFailureHandlerImpl;
import io.seata.tm.api.FailureHandler;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link SeataAutoConfiguration}.
 */
class SeataAutoConfigurationTest {

    @Test
    void testFailureHandlerCreation() {
        SeataAutoConfiguration config = new SeataAutoConfiguration();
        FailureHandler handler = config.failureHandler();
        
        assertNotNull(handler);
        assertTrue(handler instanceof DefaultFailureHandlerImpl);
    }

    @Test
    void testFailureHandlerIsDefaultImpl() {
        SeataAutoConfiguration config = new SeataAutoConfiguration();
        FailureHandler handler = config.failureHandler();
        
        assertEquals(DefaultFailureHandlerImpl.class, handler.getClass());
    }
}
