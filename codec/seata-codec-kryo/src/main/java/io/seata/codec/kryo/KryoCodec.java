package io.seata.codec.kryo;

import io.seata.common.loader.LoadLevel;
import io.seata.core.codec.Codec;

/**
 * @author jsbxyyx
 */
@LoadLevel(name = "KRYO", order = 0)
public class KryoCodec implements Codec {

    @Override
    public <T> byte[] encode(T t) {
        return new byte[0];
    }

    @Override
    public <T> T decode(byte[] bytes) {
        return null;
    }

}
