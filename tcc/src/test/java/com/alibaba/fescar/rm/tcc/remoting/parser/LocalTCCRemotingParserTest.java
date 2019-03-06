package com.alibaba.fescar.rm.tcc.remoting.parser;

import com.alibaba.fescar.rm.tcc.TccAction;
import com.alibaba.fescar.rm.tcc.TccActionImpl;
import com.alibaba.fescar.rm.tcc.remoting.RemotingDesc;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author zhangsen
 */
public class LocalTCCRemotingParserTest {

    LocalTCCRemotingParser localTCCRemotingParser = new LocalTCCRemotingParser();

    @Test
    public void testServiceParser(){
        TccActionImpl tccAction = new TccActionImpl();

        boolean result = localTCCRemotingParser.isService(tccAction, "a");
        Assert.assertTrue(result);
    }

    @Test
    public void testReferenceParser(){
        TccActionImpl tccAction = new TccActionImpl();

        boolean result = localTCCRemotingParser.isReference(tccAction, "b");
        Assert.assertTrue(result);
    }

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