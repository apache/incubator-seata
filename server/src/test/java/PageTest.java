import io.seata.core.console.result.PageResult;

import java.util.ArrayList;

public class PageTest {


    public static void main(String[] args) {
            ArrayList<String> strings = new ArrayList<>();

            PageResult<String> success = PageResult.success(strings, strings.size(), 2, 2);

            System.out.println(success);

    }
}
