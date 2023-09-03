package io.seata.serializer.protobuf.convertor;

import io.seata.core.exception.TransactionExceptionCode;
import io.seata.core.protocol.ResultCode;
import io.seata.core.protocol.transaction.BranchDeleteResponse;
import io.seata.serializer.protobuf.generated.AbstractMessageProto;
import io.seata.serializer.protobuf.generated.AbstractResultMessageProto;
import io.seata.serializer.protobuf.generated.AbstractTransactionResponseProto;
import io.seata.serializer.protobuf.generated.BranchDeleteResponseProto;
import io.seata.serializer.protobuf.generated.MessageTypeProto;
import io.seata.serializer.protobuf.generated.ResultCodeProto;
import io.seata.serializer.protobuf.generated.TransactionExceptionCodeProto;

public class BranchDeleteResponseConvertor
        implements PbConvertor<BranchDeleteResponse, BranchDeleteResponseProto> {
    @Override
    public BranchDeleteResponseProto convert2Proto(BranchDeleteResponse branchDeleteResponse) {
        final short typeCode = branchDeleteResponse.getTypeCode();

        final AbstractMessageProto abstractMessage = AbstractMessageProto.newBuilder().setMessageType(
                MessageTypeProto.forNumber(typeCode)).build();

        final String msg = branchDeleteResponse.getMsg();
        final AbstractResultMessageProto abstractResultMessageProto = AbstractResultMessageProto.newBuilder().setMsg(
                        msg == null ? "" : msg).setResultCode(ResultCodeProto.valueOf(branchDeleteResponse.getResultCode().name()))
                .setAbstractMessage(abstractMessage).build();

        AbstractTransactionResponseProto abstractTransactionResponseProto = AbstractTransactionResponseProto
                .newBuilder().setAbstractResultMessage(abstractResultMessageProto).setTransactionExceptionCode(
                        TransactionExceptionCodeProto.valueOf(branchDeleteResponse.getTransactionExceptionCode().name()))
                .build();

        BranchDeleteResponseProto result = BranchDeleteResponseProto.newBuilder().setAbstractTransactionResponse(
                abstractTransactionResponseProto).build();
        return result;
    }

    @Override
    public BranchDeleteResponse convert2Model(BranchDeleteResponseProto branchDeleteResponseProto) {
        BranchDeleteResponse branchDeleteResponse = new BranchDeleteResponse();
        final AbstractResultMessageProto abstractResultMessage = branchDeleteResponseProto
                .getAbstractTransactionResponse().getAbstractResultMessage();
        branchDeleteResponse.setMsg(abstractResultMessage.getMsg());
        branchDeleteResponse.setResultCode(ResultCode.valueOf(abstractResultMessage.getResultCode().name()));
        branchDeleteResponse.setTransactionExceptionCode(TransactionExceptionCode.valueOf(
                branchDeleteResponseProto.getAbstractTransactionResponse().getTransactionExceptionCode().name()));
        return branchDeleteResponse;
    }
}
