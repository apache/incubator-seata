package io.seata.rm.tcc.remoting.parser;

import io.seata.rm.tcc.TccParam;
import io.seata.rm.tcc.api.BusinessActionContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

public class DefaultRemotingParserTest {

    DefaultRemotingParser defaultRemotingParser = new DefaultRemotingParser();

    @Test
    public void testGetTwoPhaseArgs() throws Exception {
        Class<?> tccActionImpl = Class.forName("io.seata.rm.tcc.TccActionImpl");
        Class<?>[] argsCommitClasses = new Class[] {BusinessActionContext.class, TccParam.class, Integer.class};
        Method commitMethod = tccActionImpl.getMethod("commit", argsCommitClasses);
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            defaultRemotingParser.getTwoPhaseArgs(commitMethod, argsCommitClasses);
        });
        Class<?>[] argsRollbackClasses = new Class[] {BusinessActionContext.class, TccParam.class};
        Method rollbackMethod = tccActionImpl.getMethod("rollback", argsRollbackClasses);
        String[] keys = defaultRemotingParser.getTwoPhaseArgs(rollbackMethod, argsRollbackClasses);
        Assertions.assertNull(keys[0]);
        Assertions.assertEquals("tccParam", keys[1]);
    }

}
