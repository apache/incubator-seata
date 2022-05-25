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
package io.seata.server.session;

import io.seata.core.model.GlobalStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * the type change status validator test
 *
 * @author Bughue
 */
@SpringBootTest
public class SessionStatusValidatorTest {

    @Test
    public void testValidateUpdateStatus(){
        Assertions.assertEquals(true, SessionStatusValidator.validateUpdateStatus(GlobalStatus.Begin, GlobalStatus.Committing));
        Assertions.assertEquals(true, SessionStatusValidator.validateUpdateStatus(GlobalStatus.Committing, GlobalStatus.Committed));

        Assertions.assertEquals(false, SessionStatusValidator.validateUpdateStatus(GlobalStatus.Committing, GlobalStatus.TimeoutRollbacking));
        Assertions.assertEquals(false, SessionStatusValidator.validateUpdateStatus(GlobalStatus.TimeoutRollbacking, GlobalStatus.Committing));
        Assertions.assertEquals(false, SessionStatusValidator.validateUpdateStatus(GlobalStatus.Committing, GlobalStatus.Rollbacking));
        Assertions.assertEquals(false, SessionStatusValidator.validateUpdateStatus(GlobalStatus.Rollbacking, GlobalStatus.Committing));

        Assertions.assertEquals(false, SessionStatusValidator.validateUpdateStatus(GlobalStatus.Committed, GlobalStatus.Rollbacked));
        Assertions.assertEquals(false, SessionStatusValidator.validateUpdateStatus(GlobalStatus.Committed, GlobalStatus.TimeoutRollbacking));

    }
}
