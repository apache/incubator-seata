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
package org.apache.seata.rm.tcc.remoting.parser;

import org.apache.seata.integration.tx.api.remoting.RemotingDesc;
import org.apache.seata.rm.tcc.TccAction;
import org.apache.seata.rm.tcc.TccActionImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/**
 * The type Local tcc remoting parser test.
 *
 */
public class LocalTCCRemotingParserTest {

    /**
     * The Local tcc remoting parser.
     */
    LocalTCCRemotingParser localTCCRemotingParser = new LocalTCCRemotingParser();

    /**
     * Test service parser.
     */
    @Test
    public void testServiceParser(){
        TccActionImpl tccAction = new TccActionImpl();

        boolean result = localTCCRemotingParser.isService(tccAction, "a");
        Assertions.assertTrue(result);
    }

    /**
     * Test reference parser.
     */
    @Test
    public void testReferenceParser(){
        TccActionImpl tccAction = new TccActionImpl();

        boolean result = localTCCRemotingParser.isReference(tccAction, "b");
        Assertions.assertTrue(result);
    }

    /**
     * Test service desc.
     */
    @Test
    public void testServiceDesc(){
        TccActionImpl tccAction = new TccActionImpl();

        RemotingDesc remotingDesc = localTCCRemotingParser.getServiceDesc(tccAction, "c");
        Assertions.assertNotNull(remotingDesc);

        Assertions.assertEquals("org.apache.seata.rm.tcc.TccAction", remotingDesc.getServiceClassName());
        Assertions.assertEquals(remotingDesc.getServiceClass(), TccAction.class);
        Assertions.assertEquals(remotingDesc.getTargetBean(), tccAction);
    }

}
