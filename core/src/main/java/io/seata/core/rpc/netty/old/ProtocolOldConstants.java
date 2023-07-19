package io.seata.core.rpc.netty.old;

/**
 * ?
 *
 * @author minghua.xie
 * @date 2023/7/18
 **/
public class ProtocolOldConstants {

    public static int HEAD_LENGTH = 14;
    public static final int FLAG_REQUEST = 0x80;
    public static final int FLAG_ASYNC = 0x40;
    public static final int FLAG_HEARTBEAT = 0x20;
    public static final int FLAG_SEATA_CODEC = 0x10;
}
