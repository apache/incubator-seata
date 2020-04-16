package io.seata.config.zk;

import org.I0Itec.zkclient.exception.ZkMarshallingError;
import org.I0Itec.zkclient.serialize.ZkSerializer;

import java.nio.charset.StandardCharsets;

/**
 * Default zk serializer.
 * <p>
 * If the user is not configured in config.zk.serializer, then use default serializer.
 *
 * @author zhangchenghui.dev@gmail.com
 * @since 1.3.0
 */
public class DefaultZkSerializer implements ZkSerializer {

    @Override
    public byte[] serialize(Object data) throws ZkMarshallingError {
        return String.valueOf(data).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public Object deserialize(byte[] bytes) throws ZkMarshallingError {
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
