package adpter;

import com.demo.adapter.TimesController;
import org.junit.jupiter.api.Test;


public class TimesControllerTest {

    @Test
    public void timesTest(){
        int times = 3;
        TimesController timesController =
                TimesController.builder()
                        .sender(() -> {
                            System.out.println("Hello Lambda!");
                            return "one task is over";
                        })
                        .build()
                        .start(times);
        System.out.println("The main function is over");
        try {
            Thread.sleep(10000000000l);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void timesTest1(){
        int times = 3;
        TimesController timesController =
                TimesController.builder()
                        .sender(() -> {
                            int i = 1/0;
                            return "one task is over";
                        })
                        .build()
                        .start(times);
        System.out.println("The main function is over");
        try {
            Thread.sleep(10000000000l);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
