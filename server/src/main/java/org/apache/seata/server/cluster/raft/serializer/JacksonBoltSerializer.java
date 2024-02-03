package org.apache.seata.server.cluster.raft.serializer;

import com.alipay.remoting.exception.CodecException;
import com.alipay.remoting.serialization.Serializer;
import org.apache.seata.common.loader.EnhancedServiceLoader;
import org.apache.seata.core.serializer.SerializerType;

public class JacksonBoltSerializer implements Serializer {

    private final org.apache.seata.core.serializer.Serializer seataSerializer =
        EnhancedServiceLoader.load(org.apache.seata.core.serializer.Serializer.class,
            SerializerType.getByCode(SerializerType.JACKSON.getCode()).name());

    @Override
    public byte[] serialize(Object obj) throws CodecException {
        return seataSerializer.serialize(obj);
    }

    @Override
    public <T> T deserialize(byte[] data, String classOfT) throws CodecException {
        return seataSerializer.deserialize(data);
    }

}
