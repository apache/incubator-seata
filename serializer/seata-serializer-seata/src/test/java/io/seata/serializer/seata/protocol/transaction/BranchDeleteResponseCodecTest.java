package io.seata.serializer.seata.protocol.transaction;

import io.seata.core.exception.TransactionExceptionCode;
import io.seata.core.model.BranchStatus;
import io.seata.core.protocol.ResultCode;
import io.seata.core.protocol.transaction.BranchDeleteResponse;
import io.seata.serializer.seata.SeataSerializer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class BranchDeleteResponseCodecTest {

    /**
     * The Seata codec.
     */
    SeataSerializer seataSerializer = new SeataSerializer();

    /**
     * Test codec.
     */
    @Test
    public void test_codec(){
        BranchDeleteResponse branchDeleteResponse = new BranchDeleteResponse();
        branchDeleteResponse.setMsg("test");
        branchDeleteResponse.setResultCode(ResultCode.Failed);
        branchDeleteResponse.setTransactionExceptionCode(TransactionExceptionCode.BranchTransactionNotExist);
        branchDeleteResponse.setBranchStatus(BranchStatus.PhaseTwo_CommitFailed_XAER_NOTA_Retryable);

        byte[] bytes = seataSerializer.serialize(branchDeleteResponse);

        BranchDeleteResponse branchDeleteResponse2 = seataSerializer.deserialize(bytes);

        assertThat(branchDeleteResponse2.getMsg()).isEqualTo(branchDeleteResponse.getMsg());
        assertThat(branchDeleteResponse2.getResultCode()).isEqualTo(branchDeleteResponse.getResultCode());
        assertThat(branchDeleteResponse2.getTransactionExceptionCode()).isEqualTo(branchDeleteResponse.getTransactionExceptionCode());
        assertThat(branchDeleteResponse2.getBranchStatus()).isEqualTo(branchDeleteResponse.getBranchStatus());
    }
}
