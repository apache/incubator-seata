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
package io.seata.spring.remoting.parser;

import io.seata.spring.remoting.LocalServiceAction;
import io.seata.spring.remoting.LocalServiceActionImpl;
import io.seata.spring.remoting.RemotingDesc;
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
     * Test service desc.
     */
    @Test
    public void testServiceDesc() {
        LocalServiceAction localServiceAction = new LocalServiceActionImpl();

        RemotingDesc remotingDesc = localServiceRemotingParser.getServiceDesc(localServiceAction, "c");
        Assertions.assertNotNull(remotingDesc);

        Assertions.assertEquals("io.seata.spring.remoting.LocalServiceAction", remotingDesc.getInterfaceClassName());
        Assertions.assertEquals(remotingDesc.getInterfaceClass(), LocalServiceAction.class);
        Assertions.assertEquals(remotingDesc.getTargetBean(), localServiceAction);
    }

}
