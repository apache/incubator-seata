package verify;

import com.demo.utils.Yamls;
import org.junit.jupiter.api.Test;

import java.io.IOException;


public class TestPeopleMatcher {

    @Test
    public void testPeopleMatcher() throws IOException {
        People mark = new People();
        mark.setName("mark");
        mark.setYear(32);
        PeopleMatcher as = Yamls.load("verify-test.yml").as(PeopleMatcher.class);
        as.verify(mark);
    }
}

