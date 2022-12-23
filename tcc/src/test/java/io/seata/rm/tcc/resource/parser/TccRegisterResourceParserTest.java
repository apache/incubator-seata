package io.seata.rm.tcc.resource.parser;

import io.seata.commonapi.api.BusinessActionContext;
import io.seata.rm.tcc.TccParam;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

/**
 * @author leezongjie
 * @date 2022/12/23
 */
class TccRegisterResourceParserTest {

    TccRegisterResourceParser tccRegisterResourceParser = new TccRegisterResourceParser();

    @Test
    public void testGetTwoPhaseArgs() throws Exception {
        Class<?> tccActionImpl = Class.forName("io.seata.rm.tcc.TccActionImpl");
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