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
package io.seata.rm.saga.remoting.parser;

import io.seata.rm.saga.SagaAction;
import io.seata.rm.saga.SagaActionImpl;
import io.seata.spring.remoting.RemotingDesc;
import io.seata.spring.remoting.parser.LocalServiceRemotingParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * The type Local service remoting parser test.
 *
 * @author ruishansun
 */
public class LocalServiceRemotingParserTest {

    /**
     * The Local service remoting parser.
     */
    LocalServiceRemotingParser localServiceRemotingParser = new LocalServiceRemotingParser();

    /**
     * Test service parser.
     */
    @Test
    public void testServiceParser() {
        SagaActionImpl sagaActionImpl = new SagaActionImpl();

        boolean result = localServiceRemotingParser.isService(sagaActionImpl, "a");
        Assertions.assertTrue(result);
    }

    /**
     * Test reference parser.
     */
    @Test
    public void testReferenceParser() {
        SagaActionImpl sagaActionImpl = new SagaActionImpl();

        boolean result = localServiceRemotingParser.isReference(sagaActionImpl, "b");
        Assertions.assertTrue(result);
    }

    /**
     * Test service desc.
     */
    @Test
    public void testServiceDesc() {
        SagaActionImpl sagaActionImpl = new SagaActionImpl();

        RemotingDesc remotingDesc = localServiceRemotingParser.getServiceDesc(sagaActionImpl, "c");
        Assertions.assertNotNull(remotingDesc);

        Assertions.assertEquals("io.seata.rm.saga.SagaAction", remotingDesc.getInterfaceClassName());
        Assertions.assertEquals(remotingDesc.getInterfaceClass(), SagaAction.class);
        Assertions.assertEquals(remotingDesc.getTargetBean(), sagaActionImpl);
    }

}
