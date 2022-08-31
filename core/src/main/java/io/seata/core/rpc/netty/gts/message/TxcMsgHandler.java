package io.seata.core.rpc.netty.gts.message;


public interface TxcMsgHandler extends MsgHandler {
    void handleMessage(long var1, String var3, String var4, String var5, String var6, BeginMessage var7, AbstractResultMessage[] var8, int var9);

    void handleMessage(long var1, String var3, String var4, String var5, String var6, BranchCommitResultMessage var7, AbstractResultMessage[] var8, int var9);

    void handleMessage(long var1, String var3, String var4, String var5, String var6, BranchRollbackResultMessage var7, AbstractResultMessage[] var8, int var9);

    void handleMessage(long var1, String var3, String var4, String var5, String var6, GlobalCommitMessage var7, AbstractResultMessage[] var8, int var9);

    void handleMessage(long var1, String var3, String var4, String var5, String var6, GlobalRollbackMessage var7, AbstractResultMessage[] var8, int var9);

    void handleMessage(long var1, String var3, String var4, String var5, String var6, RegisterMessage var7, AbstractResultMessage[] var8, int var9);

    void handleMessage(long var1, String var3, String var4, String var5, String var6, ReportStatusMessage var7, AbstractResultMessage[] var8, int var9);

    void handleMessage(long var1, String var3, String var4, String var5, String var6, BeginRetryBranchMessage var7, AbstractResultMessage[] var8, int var9);

    void handleMessage(long var1, String var3, String var4, String var5, String var6, ReportUdataMessage var7, AbstractResultMessage[] var8, int var9);

    void handleMessage(long var1, String var3, String var4, String var5, String var6, TxcMergeMessage var7, AbstractResultMessage[] var8, int var9);

    void handleMessage(long var1, String var3, String var4, String var5, String var6, QueryLockMessage var7, AbstractResultMessage[] var8, int var9);

    void handleMessage(long var1, String var3, String var4, String var5, String var6, RedressMessage var7, AbstractResultMessage[] var8, int var9);
}
