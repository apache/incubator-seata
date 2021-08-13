package trigger;

import com.demo.trigger.TestTrigger;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.atomic.AtomicInteger;


@Slf4j
public class TestTriggerTest {

    private static final AtomicInteger TIMES1 = new AtomicInteger(1);
    private static final AtomicInteger TIMES2 = new AtomicInteger(1);


    @Timeout(10)
    @TestTrigger
    @DisplayName("should retry on any failure")
    void shouldRetryOnAnyFailure() {
        while (true) {
            if (TIMES1.getAndIncrement() == 10) {
                log.info("Test passed");
                break;
            }
        }
    }

    @Timeout(10)
    @TestTrigger(value = 1, throwable = ArithmeticException.class)
    void shouldRetryOnSpecficExceptionAndOneTime() {
        int i = 1 / 0;
    }


    @Timeout(10)
    @TestTrigger(3)
    @DisplayName("should retry specific times")
    void shouldRetrySpecificTimes() {
        if (TIMES2.getAndIncrement() < 3) {
            log.info("Test passed");
        }
    }

}
