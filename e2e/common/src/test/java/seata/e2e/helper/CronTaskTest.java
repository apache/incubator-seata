package seata.e2e.helper;

import org.junit.Test;

/**
 * @author xjl
 * @Description:
 */
public class CronTaskTest {

    @Test
    public void cronTest() throws InterruptedException {
        CronTask task = new CronTask(1000, () -> {
            String res = "Test";
            System.out.println(res);
            return res;
        });
        task.start();
        Thread.sleep(5000);
        task.stop();
        System.out.println("over");
    }
}
