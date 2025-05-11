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

package org.apache.seata.namingserver.smoke;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.apache.seata.namingserver.NamingserverApplication;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@SpringBootTest(
        classes = NamingserverApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"console.user.username=seata", "console.user.password=foo"})
@ExtendWith(OutputCaptureExtension.class)
class NamingControllerPropertiesSmokeTest {

    @Test
    void processShouldNotPrintLogsAndGeneratePasswordWhenPasswordIsDefined(CapturedOutput output) {
        String logs = output.getOut();
        assertFalse(logs.contains("No password was configured."));
    }
}
