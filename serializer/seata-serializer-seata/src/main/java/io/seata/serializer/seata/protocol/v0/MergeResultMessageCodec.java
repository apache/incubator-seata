package io.seata.serializer.seata.protocol.v0;

/**
 * ?
 *
 * @author Bughue
 * @date 2023/9/21
 **/
public class MergeResultMessageCodec extends io.seata.serializer.seata.protocol.v1.MergeResultMessageCodec {
    public MergeResultMessageCodec(){
        this.factory = new MessageCodecFactoryV0();
    }
}
