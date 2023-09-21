package io.seata.serializer.seata.protocol.v0;

/**
 * ?
 *
 * @author Bughue
 * @date 2023/9/21
 **/
public class BatchResultMessageCodec extends io.seata.serializer.seata.protocol.v1.BatchResultMessageCodec {
    public BatchResultMessageCodec(){
        this.factory = new MessageCodecFactoryV0();
    }
}
