package io.seata.server.session;

import io.seata.core.model.GlobalStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * the type change status validator test
 *
 * @author Bughue
 */
public class ChangeStatusValidatorTest {

    @Test
    public void testValidateUpdateStatus(){
        Assertions.assertEquals(true, ChangeStatusValidator.validateUpdateStatus(GlobalStatus.Begin, GlobalStatus.Committing));
        Assertions.assertEquals(true, ChangeStatusValidator.validateUpdateStatus(GlobalStatus.Committing, GlobalStatus.Committed));

        Assertions.assertEquals(false, ChangeStatusValidator.validateUpdateStatus(GlobalStatus.Committing, GlobalStatus.TimeoutRollbacking));
        Assertions.assertEquals(false, ChangeStatusValidator.validateUpdateStatus(GlobalStatus.TimeoutRollbacking, GlobalStatus.Committing));
        Assertions.assertEquals(false, ChangeStatusValidator.validateUpdateStatus(GlobalStatus.Committing, GlobalStatus.Rollbacking));
        Assertions.assertEquals(false, ChangeStatusValidator.validateUpdateStatus(GlobalStatus.Rollbacking, GlobalStatus.Committing));

        Assertions.assertEquals(false, ChangeStatusValidator.validateUpdateStatus(GlobalStatus.Committed, GlobalStatus.Rollbacked));
        Assertions.assertEquals(false, ChangeStatusValidator.validateUpdateStatus(GlobalStatus.Committed, GlobalStatus.TimeoutRollbacking));

    }
}
