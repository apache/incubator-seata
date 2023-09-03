package io.seata.serializer.protobuf.convertor;

import io.seata.core.model.BranchType;
import io.seata.core.protocol.transaction.BranchDeleteRequest;
import io.seata.serializer.protobuf.generated.AbstractMessageProto;
import io.seata.serializer.protobuf.generated.AbstractTransactionRequestProto;
import io.seata.serializer.protobuf.generated.BranchDeleteRequestProto;
import io.seata.serializer.protobuf.generated.BranchTypeProto;
import io.seata.serializer.protobuf.generated.MessageTypeProto;

public class BranchDeleteRequestConvertor
        implements PbConvertor<BranchDeleteRequest, BranchDeleteRequestProto> {
    @Override
    public BranchDeleteRequestProto convert2Proto(BranchDeleteRequest branchDeleteRequest) {
        final short typeCode = branchDeleteRequest.getTypeCode();

        final AbstractMessageProto abstractMessage = AbstractMessageProto.newBuilder().setMessageType(
                MessageTypeProto.forNumber(typeCode)).build();

        final AbstractTransactionRequestProto abstractTransactionRequestProto = AbstractTransactionRequestProto
                .newBuilder().setAbstractMessage(abstractMessage).build();

        final String resourceId = branchDeleteRequest.getResourceId();
        BranchDeleteRequestProto result = BranchDeleteRequestProto.newBuilder().setAbstractTransactionRequest(
                abstractTransactionRequestProto).setXid(branchDeleteRequest.getXid()).setBranchId(
                branchDeleteRequest.getBranchId()).setBranchType(BranchTypeProto.valueOf(
                branchDeleteRequest.getBranchType().name())).setResourceId(resourceId == null ? "" : resourceId).build();
        return result;
    }

    @Override
    public BranchDeleteRequest convert2Model(BranchDeleteRequestProto branchDeleteRequestProto) {
        BranchDeleteRequest branchDeleteRequest = new BranchDeleteRequest();
        branchDeleteRequest.setBranchId(branchDeleteRequestProto.getBranchId());
        branchDeleteRequest.setResourceId(branchDeleteRequestProto.getResourceId());
        branchDeleteRequest.setXid(branchDeleteRequestProto.getXid());
        branchDeleteRequest.setBranchType(
                BranchType.valueOf(branchDeleteRequestProto.getBranchType().name()));
        return branchDeleteRequest;
    }
}
