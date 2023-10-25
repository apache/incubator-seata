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

package io.seata.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Junduo Dong
 */
public class ConfigValidatorTest {

    @Test
    public void validateSuccess() {
        ConfigValidator.ValidateResult validateResult = ConfigValidator.validateRegistryConf("registry.type", "nacos");
        Assertions.assertTrue(validateResult.getValid());

        validateResult = ConfigValidator.validateCenterConf("config.type", "nacos");
        Assertions.assertTrue(validateResult.getValid());

        validateResult = ConfigValidator.validateConfiguration("server.servicePort", "8080");
        Assertions.assertTrue(validateResult.getValid());
    }

    @Test
    public void validateWrongKey() {
        ConfigValidator.ValidateResult validateResult = ConfigValidator.validateRegistryConf("aaa.bbb", "ccc");
        Assertions.assertFalse(validateResult.getValid());

        validateResult = ConfigValidator.validateCenterConf("aaa.bbb", "ccc");
        Assertions.assertFalse(validateResult.getValid());

        validateResult = ConfigValidator.validateConfiguration("aaa.bbb", "ccc");
        Assertions.assertFalse(validateResult.getValid());
    }

    @Test
    public void validateWrongValue() {
        ConfigValidator.ValidateResult validateResult = ConfigValidator.validateRegistryConf("registry.type", "wrong-type");
        Assertions.assertFalse(validateResult.getValid());

        validateResult = ConfigValidator.validateCenterConf("server.servicePort", "-1");
        Assertions.assertFalse(validateResult.getValid());

        validateResult = ConfigValidator.validateCenterConf("server.servicePort", "1.1");
        Assertions.assertFalse(validateResult.getValid());

        validateResult = ConfigValidator.validateConfiguration("metrics.enabled", "ccc");
        Assertions.assertFalse(validateResult.getValid());
    }

    @Test
    public void canBeConfigureDynamically() {
        Assertions.assertTrue(ConfigValidator.canBeConfiguredDynamically("server.undo.logSaveDays"));
        Assertions.assertFalse(ConfigValidator.canBeConfiguredDynamically("server.servicePort"));
    }
}
