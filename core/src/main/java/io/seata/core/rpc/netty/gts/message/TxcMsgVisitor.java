package io.seata.core.rpc.netty.gts.message;

public interface TxcMsgVisitor {
    void handleMessage(long var1, String var3, String var4, String var5, String var6, TxcMessage var7, AbstractResultMessage[] var8, int var9);
}