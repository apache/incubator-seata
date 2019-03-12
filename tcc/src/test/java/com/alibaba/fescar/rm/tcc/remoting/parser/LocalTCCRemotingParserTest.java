/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
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
package com.alibaba.fescar.rm.tcc.remoting.parser;

import com.alibaba.fescar.rm.tcc.TccAction;
import com.alibaba.fescar.rm.tcc.TccActionImpl;
import com.alibaba.fescar.rm.tcc.remoting.RemotingDesc;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * The type Local tcc remoting parser test.
 *
 * @author zhangsen
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
        Assert.assertTrue(result);
    }

    /**
     * Test reference parser.
     */
    @Test
    public void testReferenceParser(){
        TccActionImpl tccAction = new TccActionImpl();

        boolean result = localTCCRemotingParser.isReference(tccAction, "b");
        Assert.assertTrue(result);
    }

    /**
     * Test service desc.
     */
    @Test
    public void testServiceDesc(){
        TccActionImpl tccAction = new TccActionImpl();

        RemotingDesc remotingDesc = localTCCRemotingParser.getServiceDesc(tccAction, "c");
        Assert.assertNotNull(remotingDesc);

        Assert.assertEquals(remotingDesc.getInterfaceClassName(), "com.alibaba.fescar.rm.tcc.TccAction");
        Assert.assertEquals(remotingDesc.getInterfaceClass(), TccAction.class);
        Assert.assertEquals(remotingDesc.getTargetBean(), tccAction);
    }

}