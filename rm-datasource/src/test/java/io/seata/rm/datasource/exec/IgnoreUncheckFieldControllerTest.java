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
package io.seata.rm.datasource.exec;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * author doubleDimple lovele.cn@gmail.com
 */
public class IgnoreUncheckFieldControllerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(IgnoreUncheckFieldControllerTest.class);

    @Test
    void testRetryNotExceeded() {

        assertDoesNotThrow(() -> {
            IgnoreUncheckFieldController.getInstance().createMapCheckFields();
            LOGGER.info("result:[{}]");
        }, "should not throw anything when retry not exceeded");
    }
}
