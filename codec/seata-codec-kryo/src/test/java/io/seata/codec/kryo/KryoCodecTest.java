package io.seata.codec.kryo;

import io.seata.core.exception.TransactionExceptionCode;
import io.seata.core.model.BranchStatus;
import io.seata.core.model.BranchType;
import io.seata.core.protocol.ResultCode;
import io.seata.core.protocol.transaction.BranchCommitRequest;
import io.seata.core.protocol.transaction.BranchCommitResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author jsbxyyx
 */
public class KryoCodecTest {

    private static KryoCodec kryoCodec;

    @BeforeAll
    public static void before() {
        kryoCodec = new KryoCodec();
    }

    @Test
    public void testBranchCommitRequest() {

        BranchCommitRequest branchCommitRequest = new BranchCommitRequest();
        branchCommitRequest.setBranchType(BranchType.AT);
        branchCommitRequest.setXid("xid");
        branchCommitRequest.setResourceId("resourceId");
        branchCommitRequest.setBranchId(20190809);
        branchCommitRequest.setApplicationData("app");

        byte[] bytes = kryoCodec.encode(branchCommitRequest);
        BranchCommitRequest t = kryoCodec.decode(bytes);

        assertThat(t.getTypeCode()).isEqualTo(branchCommitRequest.getTypeCode());
        assertThat(t.getBranchType()).isEqualTo(branchCommitRequest.getBranchType());
        assertThat(t.getXid()).isEqualTo(branchCommitRequest.getXid());
        assertThat(t.getResourceId()).isEqualTo(branchCommitRequest.getResourceId());
        assertThat(t.getBranchId()).isEqualTo(branchCommitRequest.getBranchId());
        assertThat(t.getApplicationData()).isEqualTo(branchCommitRequest.getApplicationData());

    }

    @Test
    public void testBranchCommitResponse() {

        BranchCommitResponse branchCommitResponse = new BranchCommitResponse();
        branchCommitResponse.setTransactionExceptionCode(TransactionExceptionCode.BranchTransactionNotExist);
        branchCommitResponse.setBranchId(20190809);
        branchCommitResponse.setBranchStatus(BranchStatus.PhaseOne_Done);
        branchCommitResponse.setMsg("20190809");
        branchCommitResponse.setXid("20190809");
        branchCommitResponse.setResultCode(ResultCode.Failed);

        byte[] bytes = kryoCodec.encode(branchCommitResponse);
        BranchCommitResponse t = kryoCodec.decode(bytes);

        assertThat(t.getTransactionExceptionCode()).isEqualTo(branchCommitResponse.getTransactionExceptionCode());
        assertThat(t.getBranchId()).isEqualTo(branchCommitResponse.getBranchId());
        assertThat(t.getBranchStatus()).isEqualTo(branchCommitResponse.getBranchStatus());
        assertThat(t.getMsg()).isEqualTo(branchCommitResponse.getMsg());
        assertThat(t.getResultCode()).isEqualTo(branchCommitResponse.getResultCode());

    }

}
