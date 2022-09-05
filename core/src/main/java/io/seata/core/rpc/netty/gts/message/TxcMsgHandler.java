/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
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
