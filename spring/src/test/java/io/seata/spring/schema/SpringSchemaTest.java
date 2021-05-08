package io.seata.spring.schema;


import io.seata.spring.annotation.GlobalTransactionScanner;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * The type spring schema test
 *
 * @author xingfudeshi@gmail.com
 */
public class SpringSchemaTest {
    private static ClassPathXmlApplicationContext applicationContext;


    @BeforeAll
    private static void init() {
        applicationContext = new ClassPathXmlApplicationContext("classpath:seata.xml");
    }

    @Test
    public void testGetGlobalTransactionScanner() {
        GlobalTransactionScanner globalTransactionScanner = applicationContext.getBean(GlobalTransactionScanner.class);
        Assertions.assertThat(globalTransactionScanner).isNotNull();
    }

    @Test
    public void testGtxTargetHolder() {
        GtxTarget gtxTarget = GtxTargetHolder.INSTANCE.find(GtxTargetType.METHOD, "doBiz");
        Assertions.assertThat(gtxTarget).isNotNull();
        Assertions.assertThat(gtxTarget.getTargetName()).isEqualTo("doBiz");
    }
}
