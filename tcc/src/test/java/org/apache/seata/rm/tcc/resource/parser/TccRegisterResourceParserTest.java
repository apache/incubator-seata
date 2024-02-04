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
package org.apache.seata.rm.tcc.resource.parser;

import org.apache.seata.rm.tcc.TccParam;
import org.apache.seata.rm.tcc.api.BusinessActionContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;


class TccRegisterResourceParserTest {

    TccRegisterResourceParser tccRegisterResourceParser = new TccRegisterResourceParser();

    @Test
    public void testGetTwoPhaseArgs() throws Exception {
        Class<?> tccActionImpl = Class.forName("org.apache.seata.rm.tcc.TccActionImpl");
        Class<?>[] argsCommitClasses = new Class[]{BusinessActionContext.class, TccParam.class, Integer.class};
        Method commitMethod = tccActionImpl.getMethod("commit", argsCommitClasses);
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            tccRegisterResourceParser.getTwoPhaseArgs(commitMethod, argsCommitClasses);
        });
        Class<?>[] argsRollbackClasses = new Class[]{BusinessActionContext.class, TccParam.class};
        Method rollbackMethod = tccActionImpl.getMethod("rollback", argsRollbackClasses);
        String[] keys = tccRegisterResourceParser.getTwoPhaseArgs(rollbackMethod, argsRollbackClasses);
        Assertions.assertNull(keys[0]);
        Assertions.assertEquals("tccParam", keys[1]);
    }
}
