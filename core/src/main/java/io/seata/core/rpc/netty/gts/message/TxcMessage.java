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

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

public abstract class TxcMessage implements TxcMsgVisitor, Serializable, TxcCodec {
    private static final long serialVersionUID = -1441020418526899889L;
    public static final short TYPE_BEGIN = 1;
    public static final short TYPE_BEGIN_RESULT = 2;
    public static final short TYPE_BRANCH_COMMIT = 3;
    public static final short TYPE_BRANCH_COMMIT_RESULT = 4;
    public static final short TYPE_BRANCH_ROLLBACK = 5;
    public static final short TYPE_BRANCH_ROLLBACK_RESULT = 6;
    public static final short TYPE_GLOBAL_COMMIT = 7;
    public static final short TYPE_GLOBAL_COMMIT_RESULT = 8;
    public static final short TYPE_GLOBAL_ROLLBACK = 9;
    public static final short TYPE_GLOBAL_ROLLBACK_RESULT = 10;
    public static final short TYPE_REGIST = 11;
    public static final short TYPE_REGIST_RESULT = 12;
    public static final short TYPE_REPORT_STATUS = 13;
    public static final short TYPE_REPORT_STATUS_RESULT = 14;
    public static final short TYPE_BEGIN_RETRY_BRANCH = 15;
    public static final short TYPE_BEGIN_RETRY_BRANCH_RESULT = 16;
    public static final short TYPE_REPORT_UDATA = 17;
    public static final short TYPE_REPORT_UDATA_RESULT = 18;
    public static final short TYPE_TXC_MERGE = 19;
    public static final short TYPE_TXC_MERGE_RESULT = 20;
    public static final short TYPE_QUERY_LOCK = 21;
    public static final short TYPE_QUERY_LOCK_RESULT = 22;
    public static final short TYPE_REG_CLT = 101;
    public static final short TYPE_REG_CLT_RESULT = 102;
    public static final short TYPE_REG_RM = 103;
    public static final short TYPE_REG_RM_RESULT = 104;
    public static final short TYPE_REG_CLUSTER_NODE = 105;
    public static final short TYPE_REG_CLUSTER_NODE_RESULT = 106;
    public static final short TYPE_CLUSTER_BRANCH = 107;
    public static final short TYPE_CLUSTER_BRANCH_RESULT = 108;
    public static final short TYPE_CLUSTER_GLOBAL = 109;
    public static final short TYPE_CLUSTER_GLOBAL_RESULT = 110;
    public static final short TYPE_CLUSTER_SYNC = 111;
    public static final short TYPE_CLUSTER_SYNC_RESULT = 112;
    public static final short TYPE_CLUSTER_DUMP = 113;
    public static final short TYPE_CLUSTER_DUMP_RESULT = 114;
    public static final short TYPE_CLUSTER_MERGE = 115;
    public static final short TYPE_CLUSTER_MERGE_RESULT = 116;
    public static final short TYPE_CLUSTER_QUERY_LOCK = 117;
    public static final short TYPE_CLUSTER_QUERY_LOCK_RESULT = 118;
    public static final short TYPE_CLUSTER_ALARM = 119;
    public static final short TYPE_CLUSTER_ALARM_RESULT = 120;
    public static final short TYPE_REDRESS = 121;
    public static final short TYPE_REDRESS_RESULT = 122;
    public static final short TYPE_CLUSTER_BKUP = 123;
    public static final short TYPE_CLUSTER_BKUP_RESULT = 124;
    public static final Charset UTF8 = Charset.forName("utf-8");
    public MsgHandler handler;
    public ChannelHandlerContext ctx;
    public static final Map<Short, String> typeMap = new HashMap();

    public TxcMessage() {
    }

    public MsgHandler getHandler() {
        return this.handler;
    }

    public void setHandler(MsgHandler handler) {
        this.handler = handler;
    }

    public static int bytesToInt(byte[] bytes, int offset) {
        int ret = 0;

        for (int i = 0; i < 4 && i + offset < bytes.length; ++i) {
            ret <<= 8;
            ret |= bytes[i + offset] & 255;
        }

        return ret;
    }

    public static void intToBytes(int i, byte[] bytes, int offset) {
        bytes[offset] = (byte) (i >> 24 & 255);
        bytes[offset + 1] = (byte) (i >> 16 & 255);
        bytes[offset + 2] = (byte) (i >> 8 & 255);
        bytes[offset + 3] = (byte) (i & 255);
    }

    @Override
    public void handleMessage(long msgId, String dbKeys, String clientIp, String clientAppName, String vgroupName, TxcMessage message, AbstractResultMessage[] results, int idx) {
    }

    @Override
    public void setChannelHandlerContext(ChannelHandlerContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public boolean decode(ByteBuf in) {
        return false;
    }

    static {
        typeMap.put((short) 1, BeginMessage.class.getName());
        typeMap.put((short) 2, BeginResultMessage.class.getName());
        typeMap.put((short) 3, BranchCommitMessage.class.getName());
        typeMap.put((short) 4, BranchCommitResultMessage.class.getName());
        typeMap.put((short) 5, BranchRollbackMessage.class.getName());
        typeMap.put((short) 6, BranchRollbackResultMessage.class.getName());
        typeMap.put((short) 7, GlobalCommitMessage.class.getName());
        typeMap.put((short) 8, GlobalCommitResultMessage.class.getName());
        typeMap.put((short) 9, GlobalRollbackMessage.class.getName());
        typeMap.put((short) 10, GlobalRollbackResultMessage.class.getName());
        typeMap.put((short) 11, RegisterMessage.class.getName());
        typeMap.put((short) 12, RegisterResultMessage.class.getName());
        typeMap.put((short) 13, ReportStatusMessage.class.getName());
        typeMap.put((short) 14, ReportStatusResultMessage.class.getName());
        typeMap.put((short) 15, BeginRetryBranchMessage.class.getName());
        typeMap.put((short) 16, BeginRetryBranchResultMessage.class.getName());
        typeMap.put((short) 17, ReportUdataMessage.class.getName());
        typeMap.put((short) 18, ReportUdataResultMessage.class.getName());
        typeMap.put((short) 19, TxcMergeMessage.class.getName());
        typeMap.put((short) 20, TxcMergeResultMessage.class.getName());
        typeMap.put((short) 21, QueryLockMessage.class.getName());
        typeMap.put((short) 22, QueryLockResultMessage.class.getName());
        typeMap.put((short) 101, "com.taobao.txc.rpc.impl.RegisterClientAppNameMessage");
        typeMap.put((short) 102, "com.taobao.txc.rpc.impl.RegisterClientAppNameResultMessage");
        typeMap.put((short) 103, "com.taobao.txc.rpc.impl.RegisterRmMessage");
        typeMap.put((short) 104, "com.taobao.txc.rpc.impl.RegisterRmResultMessage");
        typeMap.put((short) 105, "com.taobao.txc.cluster.message.RegisterClusterNodeMessage");
        typeMap.put((short) 106, "com.taobao.txc.cluster.message.RegisterClusterNodeResultMessage");
        typeMap.put((short) 107, "com.taobao.txc.cluster.message.ClusterBranchMessage");
        typeMap.put((short) 108, "com.taobao.txc.cluster.message.ClusterBranchResultMessage");
        typeMap.put((short) 109, "com.taobao.txc.cluster.message.ClusterGlobalMessage");
        typeMap.put((short) 110, "com.taobao.txc.cluster.message.ClusterGlobalResultMessage");
        typeMap.put((short) 111, "com.taobao.txc.cluster.message.ClusterSyncMessage");
        typeMap.put((short) 112, "com.taobao.txc.cluster.message.ClusterSyncResultMessage");
        typeMap.put((short) 113, "com.taobao.txc.message.ClusterDumpMessage");
        typeMap.put((short) 114, "com.taobao.txc.message.ClusterDumpResultMessage");
        typeMap.put((short) 115, "com.taobao.txc.cluster.message.ClusterMergeMessage");
        typeMap.put((short) 116, "com.taobao.txc.cluster.message.ClusterMergeResultMessage");
        typeMap.put((short) 117, "com.taobao.txc.cluster.message.ClusterQueryLockMessage");
        typeMap.put((short) 118, "com.taobao.txc.cluster.message.ClusterQueryLockResultMessage");
        typeMap.put((short) 119, "com.taobao.txc.cluster.message.ClusterAlarmMessage");
        typeMap.put((short) 120, "com.taobao.txc.cluster.message.ClusterAlarmResultMessage");
        typeMap.put((short) 121, RedressMessage.class.getName());
        typeMap.put((short) 122, RedressResultMessage.class.getName());
        typeMap.put((short) 123, "com.taobao.txc.cluster.message.ClusterBkupMessage");
        typeMap.put((short) 124, "com.taobao.txc.cluster.message.ClusterBkupResultMessage");
    }
}
