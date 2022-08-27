package io.seata.core.rpc.grpc;

import com.google.protobuf.Message;
import io.seata.serializer.protobuf.convertor.PbConvertor;
import io.seata.serializer.protobuf.manager.ProtobufConvertManager;

/**
 * @author goodboycoder
 */
public class ProtoTypeConvertHelper {

    @SuppressWarnings("unchecked")
    public static Message convertToProto(Object model) {
        PbConvertor pbConvertor = ProtobufConvertManager.getInstance().fetchConvertor(model.getClass().getName());
        return (Message) pbConvertor.convert2Proto(model);
    }

    @SuppressWarnings("unchecked")
    public static Object convertToModel(Message protoMsg) {
        PbConvertor pbConvertor = ProtobufConvertManager.getInstance().fetchConvertor(protoMsg.getClass().getName());
        return pbConvertor.convert2Model(protoMsg);
    }
}
