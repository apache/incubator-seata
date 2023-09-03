package io.seata.serializer.protobuf.convertor;

import io.seata.core.exception.TransactionExceptionCode;
import io.seata.core.protocol.ResultCode;
import io.seata.core.protocol.transaction.BranchDeleteResponse;
import io.seata.serializer.protobuf.generated.BranchDeleteResponseProto;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class BranchDeleteResponseConvertorTest {
    @Test
    public void convert2Proto() {
        BranchDeleteResponse branchDeleteResponse = new BranchDeleteResponse();
        branchDeleteResponse.setMsg("msg");
        branchDeleteResponse.setResultCode(ResultCode.Failed);
        branchDeleteResponse.setTransactionExceptionCode(TransactionExceptionCode.GlobalTransactionNotExist);
        BranchDeleteResponseConvertor convertor = new BranchDeleteResponseConvertor();
        BranchDeleteResponseProto proto = convertor.convert2Proto(branchDeleteResponse);
        BranchDeleteResponse real = convertor.convert2Model(proto);
        assertThat((real.getTypeCode())).isEqualTo(branchDeleteResponse.getTypeCode());
        assertThat((real.getMsg())).isEqualTo(branchDeleteResponse.getMsg());
        assertThat((real.getResultCode())).isEqualTo(branchDeleteResponse.getResultCode());
        assertThat((real.getTransactionExceptionCode())).isEqualTo(branchDeleteResponse.getTransactionExceptionCode());

    }
}
