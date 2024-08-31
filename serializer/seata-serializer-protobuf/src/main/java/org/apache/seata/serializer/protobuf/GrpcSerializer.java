package org.apache.seata.serializer.protobuf;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import org.apache.seata.common.exception.ShouldNeverHappenException;
import org.apache.seata.common.loader.LoadLevel;
import org.apache.seata.core.serializer.Serializer;
import org.apache.seata.serializer.protobuf.convertor.PbConvertor;
import org.apache.seata.serializer.protobuf.manager.ProtobufConvertManager;

@LoadLevel(name = "GRPC")
public class GrpcSerializer implements Serializer {
    @Override
    public <T> byte[] serialize(T t) {
        PbConvertor pbConvertor = ProtobufConvertManager.getInstance()
                .fetchConvertor(t.getClass().getName());
        Any grpcBody = Any.pack((Message) pbConvertor.convert2Proto(t));

        return grpcBody.toByteArray();
    }

    @Override
    public <T> T deserialize(byte[] bytes) {
        try {
            Any body = Any.parseFrom(bytes);
            final Class clazz = ProtobufConvertManager.getInstance().fetchProtoClass(getTypeNameFromTypeUrl(body.getTypeUrl()));
            if (body.is(clazz)) {
                Object ob = body.unpack(clazz);
                PbConvertor pbConvertor = ProtobufConvertManager.getInstance().fetchReversedConvertor(clazz.getName());

                return (T) pbConvertor.convert2Model(ob);
            }
        } catch (Throwable e) {
            throw new ShouldNeverHappenException("GrpcSerializer deserialize error", e);
        }

        return null;
    }

    private String getTypeNameFromTypeUrl(String typeUri) {
        int pos = typeUri.lastIndexOf('/');
        return pos == -1 ? "" : typeUri.substring(pos + 1);
    }
}
