package org.apache.seata.sqlparser.druid;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SupportSqlWhereMethodTest {

    @Test
    public void testGetInstance() {
        SupportSqlWhereMethod instance1 = SupportSqlWhereMethod.getInstance();
        SupportSqlWhereMethod instance2 = SupportSqlWhereMethod.getInstance();
        Assertions.assertInstanceOf(SupportSqlWhereMethod.class, instance1);
        Assertions.assertSame(instance1, instance2);
    }

    @Test
    public void testAddAndCheck() {
        SupportSqlWhereMethod supportSqlWhereMethod = SupportSqlWhereMethod.getInstance();

        // test default method
        Assertions.assertTrue(supportSqlWhereMethod.checkIsSupport("FIND_IN_SET"));
        Assertions.assertTrue(supportSqlWhereMethod.checkIsSupport("find_in_set"));

        // test add new method
        String newMethod = "CONCAT";
        supportSqlWhereMethod.add(newMethod);
        Assertions.assertTrue(supportSqlWhereMethod.checkIsSupport(newMethod));
        Assertions.assertTrue(supportSqlWhereMethod.checkIsSupport(newMethod.toLowerCase()));

        // test unsupported method
        Assertions.assertFalse(supportSqlWhereMethod.checkIsSupport("NOT_SUPPORT_METHOD"));
    }
}
