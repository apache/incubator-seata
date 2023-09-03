package io.seata.serializer.protobuf.convertor;

import io.seata.core.model.BranchType;
import io.seata.core.protocol.transaction.BranchDeleteRequest;
import io.seata.serializer.protobuf.generated.BranchDeleteRequestProto;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class BranchDeleteRequestConvertorTest {
    @Test
    public void convert2Proto() {
        BranchDeleteRequest branchDeleteRequest = new BranchDeleteRequest();
        branchDeleteRequest.setBranchType(BranchType.AT);
        branchDeleteRequest.setXid("xid");
        branchDeleteRequest.setResourceId("resourceId");
        branchDeleteRequest.setBranchId(123);

        BranchDeleteRequestConvertor branchDeleteRequestConvertor = new BranchDeleteRequestConvertor();
        BranchDeleteRequestProto proto = branchDeleteRequestConvertor.convert2Proto(
                branchDeleteRequest);
        BranchDeleteRequest realRequest = branchDeleteRequestConvertor.convert2Model(proto);

        assertThat(realRequest.getTypeCode()).isEqualTo(branchDeleteRequest.getTypeCode());
        assertThat(realRequest.getBranchType()).isEqualTo(branchDeleteRequest.getBranchType());
        assertThat(realRequest.getXid()).isEqualTo(branchDeleteRequest.getXid());
        assertThat(realRequest.getResourceId()).isEqualTo(branchDeleteRequest.getResourceId());
        assertThat(realRequest.getBranchId()).isEqualTo(branchDeleteRequest.getBranchId());

    }
}
