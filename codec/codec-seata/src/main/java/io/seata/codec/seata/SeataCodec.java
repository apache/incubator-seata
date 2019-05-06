package io.seata.codec.seata;

import io.seata.common.loader.LoadLevel;
import io.seata.core.codec.Codec;

/**
 * The type Seata codec.
 *
 * @author zhangsen
 * @data 2019 /5/6
 */
@LoadLevel(name="seata", order = 0)
public class SeataCodec implements Codec {

    @Override
    public <T> byte[] encode(T t) {
        return new byte[0];
    }

    @Override
    public <T> T decode(byte[] bytes) {
        return null;
    }

}
