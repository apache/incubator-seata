package io.seata.serializer.seata.protocol.v0;

/**
 * ?
 *
 * @author Bughue
 * @date 2023/9/21
 **/
public class MergedWarpMessageCodec extends io.seata.serializer.seata.protocol.v1.MergedWarpMessageCodec {
    public MergedWarpMessageCodec(){
        this.factory = new MessageCodecFactoryV0();
    }
}
