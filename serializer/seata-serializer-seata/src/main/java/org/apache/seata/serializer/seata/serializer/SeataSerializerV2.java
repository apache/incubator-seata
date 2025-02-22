package org.apache.seata.serializer.seata.serializer;

import org.apache.seata.core.protocol.ProtocolConstants;

/**
 * the type SeataSerializer(V2)
 **/
public class SeataSerializerV2 extends SeataSerializerV1{
    private static volatile SeataSerializerV2 instance;

    protected SeataSerializerV2() {
    }

    public static SeataSerializerV2 getInstance() {
        if (instance == null) {
            synchronized (SeataSerializerV2.class) {
                if (instance == null) {
                    instance = new SeataSerializerV2();
                }
            }
        }
        return instance;
    }

    public byte protocolVersion(){
        return ProtocolConstants.VERSION_2;
    }
}
