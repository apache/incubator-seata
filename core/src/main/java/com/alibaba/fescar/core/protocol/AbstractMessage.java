/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
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

package com.alibaba.fescar.core.protocol;

import java.io.Serializable;
import java.nio.charset.Charset;

import com.alibaba.fescar.core.protocol.transaction.*;
import com.alibaba.fescar.core.protocol.transaction.GlobalBeginRequest;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

/**
 * @Author: jimin.jm@alibaba-inc.com
 * @Project: fescar-all
 * @DateTime: 2018/9/14 16:54
 * @FileName: AbstractMessage
 * @Description:
 */
public abstract class AbstractMessage implements MessageCodec, Serializable {
    private static final long serialVersionUID = -1441020418526899889L;

    public static final short TYPE_GLOBAL_BEGIN = 1;
    public static final short TYPE_GLOBAL_BEGIN_RESULT = 2;
    public static final short TYPE_GLOBAL_COMMIT = 7;
    public static final short TYPE_GLOBAL_COMMIT_RESULT = 8;
    public static final short TYPE_GLOBAL_ROLLBACK = 9;
    public static final short TYPE_GLOBAL_ROLLBACK_RESULT = 10;
    public static final short TYPE_GLOBAL_STATUS = 15;
    public static final short TYPE_GLOBAL_STATUS_RESULT = 16;
    public static final short TYPE_GLOBAL_LOCK_QUERY = 21;
    public static final short TYPE_GLOBAL_LOCK_QUERY_RESULT = 22;

    public static final short TYPE_BRANCH_COMMIT = 3;
    public static final short TYPE_BRANCH_COMMIT_RESULT = 4;
    public static final short TYPE_BRANCH_ROLLBACK = 5;
    public static final short TYPE_BRANCH_ROLLBACK_RESULT = 6;
    public static final short TYPE_BRANCH_REGISTER = 11;
    public static final short TYPE_BRANCH_REGISTER_RESULT = 12;
    public static final short TYPE_BRANCH_STATUS_REPORT = 13;
    public static final short TYPE_BRANCH_STATUS_REPORT_RESULT = 14;

    public static final short TYPE_FESCAR_MERGE = 59;
    public static final short TYPE_FESCAR_MERGE_RESULT = 60;

    public static final short TYPE_REG_CLT = 101;
    public static final short TYPE_REG_CLT_RESULT = 102;
    public static final short TYPE_REG_RM = 103;
    public static final short TYPE_REG_RM_RESULT = 104;

    protected static final Charset UTF8 = Charset.forName("utf-8");
    protected ChannelHandlerContext ctx;

    public static int bytesToInt(byte[] bytes, int offset) {
        int ret = 0;
        for (int i = 0; i < 4 && i + offset < bytes.length; i++) {
            ret <<= 8;
            ret |= (int)bytes[i + offset] & 0xFF;
        }
        return ret;
    }

    public static void intToBytes(int i, byte[] bytes, int offset) {
        bytes[offset] = (byte)((i >> 24) & 0xFF);
        bytes[offset + 1] = (byte)((i >> 16) & 0xFF);
        bytes[offset + 2] = (byte)((i >> 8) & 0xFF);
        bytes[offset + 3] = (byte)(i & 0xFF);
    }

    @Override
    public boolean decode(ByteBuf in) {
        return false;
    }

    public static MessageCodec getMsgInstanceByCode(short typeCode) {
        MessageCodec msgCodec = null;
        switch (typeCode) {
            case AbstractMessage.TYPE_FESCAR_MERGE:
                msgCodec = new MergedWarpMessage();
                break;
            case AbstractMessage.TYPE_FESCAR_MERGE_RESULT:
                msgCodec = new MergeResultMessage();
                break;
            case AbstractMessage.TYPE_REG_CLT:
                msgCodec = new RegisterTMRequest();
                break;
            case AbstractMessage.TYPE_REG_CLT_RESULT:
                msgCodec = new RegisterTMResponse();
                break;
            case AbstractMessage.TYPE_REG_RM:
                msgCodec = new RegisterRMRequest();
                break;
            case AbstractMessage.TYPE_REG_RM_RESULT:
                msgCodec = new RegisterRMResponse();
                break;
            case AbstractMessage.TYPE_BRANCH_COMMIT:
                msgCodec = new BranchCommitRequest();
                break;
            case AbstractMessage.TYPE_BRANCH_ROLLBACK:
                msgCodec = new BranchRollbackRequest();
                break;
            default:
                msgCodec = null;
                break;
        }
        if (null != msgCodec) {
            return msgCodec;
        } else {
            try {
                msgCodec = (MessageCodec)getMergeRequestInstanceByCode(typeCode);
            } catch (Exception exx) {}
            if (null != msgCodec) {
                return msgCodec;
            } else {
                return (MessageCodec)getMergeResponseInstanceByCode(typeCode);
            }
        }
    }

    public static MergedMessage getMergeRequestInstanceByCode(int typeCode) {
        switch (typeCode) {
            case AbstractMessage.TYPE_GLOBAL_BEGIN:
                return new GlobalBeginRequest();
            case AbstractMessage.TYPE_GLOBAL_COMMIT:
                return new GlobalCommitRequest();
            case AbstractMessage.TYPE_GLOBAL_ROLLBACK:
                return new GlobalRollbackRequest();
            case AbstractMessage.TYPE_GLOBAL_STATUS:
                return new GlobalStatusRequest();
            case AbstractMessage.TYPE_GLOBAL_LOCK_QUERY:
                return new GlobalLockQueryRequest();
            case AbstractMessage.TYPE_BRANCH_REGISTER:
                return new BranchRegisterRequest();
            case AbstractMessage.TYPE_BRANCH_STATUS_REPORT:
                return new BranchReportRequest();
            default:
                throw new RuntimeException("not support typeCode," + typeCode);
        }
    }

    public static MergedMessage getMergeResponseInstanceByCode(int typeCode) {
        switch (typeCode) {
            case AbstractMessage.TYPE_GLOBAL_BEGIN_RESULT:
                return new GlobalBeginResponse();
            case AbstractMessage.TYPE_GLOBAL_COMMIT_RESULT:
                return new GlobalCommitResponse();
            case AbstractMessage.TYPE_GLOBAL_ROLLBACK_RESULT:
                return new GlobalRollbackResponse();
            case AbstractMessage.TYPE_GLOBAL_STATUS_RESULT:
                return new GlobalStatusResponse();
            case AbstractMessage.TYPE_GLOBAL_LOCK_QUERY_RESULT:
                return new GlobalLockQueryResponse();
            case AbstractMessage.TYPE_BRANCH_REGISTER_RESULT:
                return new BranchRegisterResponse();
            case AbstractMessage.TYPE_BRANCH_STATUS_REPORT_RESULT:
                return new BranchReportResponse();
            case AbstractMessage.TYPE_BRANCH_COMMIT_RESULT:
                return new BranchCommitResponse();
            case AbstractMessage.TYPE_BRANCH_ROLLBACK_RESULT:
                return new BranchRollbackResponse();
            default:
                throw new RuntimeException("not support typeCode," + typeCode);
        }
    }
}
