package verify;

import com.demo.verify.AbstractMatcher;
import lombok.Data;

import java.util.List;
import java.util.Objects;


@Data
public class PeopleMatcher extends AbstractMatcher<People> {

    private List<People> people;

    @Override
    public void verify(People p) {
        if (Objects.nonNull(getPeople())){
            for (People temp : this.people) {
                // whether p.getYear() is greater than temp.year.
                doVerify("gt " + temp.year, p.getYear());
            }
        }
    }
}
