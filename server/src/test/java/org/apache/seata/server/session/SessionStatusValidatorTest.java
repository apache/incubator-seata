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
package org.apache.seata.server.session;

import org.apache.seata.core.model.GlobalStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * the type change status validator test
 *
 */
@SpringBootTest
public class SessionStatusValidatorTest {

    @Test
    public void testValidateUpdateStatus(){
        Assertions.assertTrue(SessionStatusValidator.validateUpdateStatus(GlobalStatus.Begin, GlobalStatus.Committing));
        Assertions.assertTrue(
            SessionStatusValidator.validateUpdateStatus(GlobalStatus.Committing, GlobalStatus.Committed));

        Assertions.assertFalse(
            SessionStatusValidator.validateUpdateStatus(GlobalStatus.Committing, GlobalStatus.TimeoutRollbacking));
        Assertions.assertFalse(
            SessionStatusValidator.validateUpdateStatus(GlobalStatus.TimeoutRollbacking, GlobalStatus.Committing));
        Assertions.assertFalse(
            SessionStatusValidator.validateUpdateStatus(GlobalStatus.Committing, GlobalStatus.Rollbacking));
        Assertions.assertFalse(
            SessionStatusValidator.validateUpdateStatus(GlobalStatus.Rollbacking, GlobalStatus.Committing));

        Assertions.assertFalse(
            SessionStatusValidator.validateUpdateStatus(GlobalStatus.Committed, GlobalStatus.Rollbacked));
        Assertions.assertFalse(
            SessionStatusValidator.validateUpdateStatus(GlobalStatus.Committed, GlobalStatus.TimeoutRollbacking));

    }
}
