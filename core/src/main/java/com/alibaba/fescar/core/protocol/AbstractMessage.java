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

import com.alibaba.fescar.core.protocol.transaction.BranchCommitRequest;
import com.alibaba.fescar.core.protocol.transaction.BranchCommitResponse;
import com.alibaba.fescar.core.protocol.transaction.BranchRegisterRequest;
import com.alibaba.fescar.core.protocol.transaction.BranchRegisterResponse;
import com.alibaba.fescar.core.protocol.transaction.BranchReportRequest;
import com.alibaba.fescar.core.protocol.transaction.BranchReportResponse;
import com.alibaba.fescar.core.protocol.transaction.BranchRollbackRequest;
import com.alibaba.fescar.core.protocol.transaction.BranchRollbackResponse;
import com.alibaba.fescar.core.protocol.transaction.GlobalBeginRequest;
import com.alibaba.fescar.core.protocol.transaction.GlobalBeginResponse;
import com.alibaba.fescar.core.protocol.transaction.GlobalCommitRequest;
import com.alibaba.fescar.core.protocol.transaction.GlobalCommitResponse;
import com.alibaba.fescar.core.protocol.transaction.GlobalLockQueryRequest;
import com.alibaba.fescar.core.protocol.transaction.GlobalLockQueryResponse;
import com.alibaba.fescar.core.protocol.transaction.GlobalRollbackRequest;
import com.alibaba.fescar.core.protocol.transaction.GlobalRollbackResponse;
import com.alibaba.fescar.core.protocol.transaction.GlobalStatusRequest;
import com.alibaba.fescar.core.protocol.transaction.GlobalStatusResponse;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

/**
 * The type Abstract message.
 *
 * @Author: jimin.jm @alibaba-inc.com
 * @Project: fescar -all
 * @DateTime: 2018 /9/14 16:54
 * @FileName: AbstractMessage
 * @Description:
 */
public abstract class AbstractMessage implements MessageCodec, Serializable {
    private static final long serialVersionUID = -1441020418526899889L;

    /**
     * The constant TYPE_GLOBAL_BEGIN.
     */
    public static final short TYPE_GLOBAL_BEGIN = 1;
    /**
     * The constant TYPE_GLOBAL_BEGIN_RESULT.
     */
    public static final short TYPE_GLOBAL_BEGIN_RESULT = 2;
    /**
     * The constant TYPE_GLOBAL_COMMIT.
     */
    public static final short TYPE_GLOBAL_COMMIT = 7;
    /**
     * The constant TYPE_GLOBAL_COMMIT_RESULT.
     */
    public static final short TYPE_GLOBAL_COMMIT_RESULT = 8;
    /**
     * The constant TYPE_GLOBAL_ROLLBACK.
     */
    public static final short TYPE_GLOBAL_ROLLBACK = 9;
    /**
     * The constant TYPE_GLOBAL_ROLLBACK_RESULT.
     */
    public static final short TYPE_GLOBAL_ROLLBACK_RESULT = 10;
    /**
     * The constant TYPE_GLOBAL_STATUS.
     */
    public static final short TYPE_GLOBAL_STATUS = 15;
    /**
     * The constant TYPE_GLOBAL_STATUS_RESULT.
     */
    public static final short TYPE_GLOBAL_STATUS_RESULT = 16;
    /**
     * The constant TYPE_GLOBAL_LOCK_QUERY.
     */
    public static final short TYPE_GLOBAL_LOCK_QUERY = 21;
    /**
     * The constant TYPE_GLOBAL_LOCK_QUERY_RESULT.
     */
    public static final short TYPE_GLOBAL_LOCK_QUERY_RESULT = 22;

    /**
     * The constant TYPE_BRANCH_COMMIT.
     */
    public static final short TYPE_BRANCH_COMMIT = 3;
    /**
     * The constant TYPE_BRANCH_COMMIT_RESULT.
     */
    public static final short TYPE_BRANCH_COMMIT_RESULT = 4;
    /**
     * The constant TYPE_BRANCH_ROLLBACK.
     */
    public static final short TYPE_BRANCH_ROLLBACK = 5;
    /**
     * The constant TYPE_BRANCH_ROLLBACK_RESULT.
     */
    public static final short TYPE_BRANCH_ROLLBACK_RESULT = 6;
    /**
     * The constant TYPE_BRANCH_REGISTER.
     */
    public static final short TYPE_BRANCH_REGISTER = 11;
    /**
     * The constant TYPE_BRANCH_REGISTER_RESULT.
     */
    public static final short TYPE_BRANCH_REGISTER_RESULT = 12;
    /**
     * The constant TYPE_BRANCH_STATUS_REPORT.
     */
    public static final short TYPE_BRANCH_STATUS_REPORT = 13;
    /**
     * The constant TYPE_BRANCH_STATUS_REPORT_RESULT.
     */
    public static final short TYPE_BRANCH_STATUS_REPORT_RESULT = 14;

    /**
     * The constant TYPE_FESCAR_MERGE.
     */
    public static final short TYPE_FESCAR_MERGE = 59;
    /**
     * The constant TYPE_FESCAR_MERGE_RESULT.
     */
    public static final short TYPE_FESCAR_MERGE_RESULT = 60;

    /**
     * The constant TYPE_REG_CLT.
     */
    public static final short TYPE_REG_CLT = 101;
    /**
     * The constant TYPE_REG_CLT_RESULT.
     */
    public static final short TYPE_REG_CLT_RESULT = 102;
    /**
     * The constant TYPE_REG_RM.
     */
    public static final short TYPE_REG_RM = 103;
    /**
     * The constant TYPE_REG_RM_RESULT.
     */
    public static final short TYPE_REG_RM_RESULT = 104;

    /**
     * The constant UTF8.
     */
    protected static final Charset UTF8 = Charset.forName("utf-8");
    /**
     * The Ctx.
     */
    protected ChannelHandlerContext ctx;

    /**
     * Bytes to int int.
     *
     * @param bytes  the bytes
     * @param offset the offset
     * @return the int
     */
    public static int bytesToInt(byte[] bytes, int offset) {
        int ret = 0;
        for (int i = 0; i < 4 && i + offset < bytes.length; i++) {
            ret <<= 8;
            ret |= (int)bytes[i + offset] & 0xFF;
        }
        return ret;
    }

    /**
     * Int to bytes.
     *
     * @param i      the
     * @param bytes  the bytes
     * @param offset the offset
     */
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

    /**
     * Gets msg instance by code.
     *
     * @param typeCode the type code
     * @return the msg instance by code
     */
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
                break;
        }

        if (null != msgCodec) {
            return msgCodec;
        }

        try {
            msgCodec = (MessageCodec)getMergeRequestInstanceByCode(typeCode);
        } catch (Exception exx) {}

        if (null != msgCodec) {
            return msgCodec;
        }

        return (MessageCodec)getMergeResponseInstanceByCode(typeCode);
    }

    /**
     * Gets merge request instance by code.
     *
     * @param typeCode the type code
     * @return the merge request instance by code
     */
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
                throw new IllegalArgumentException("not support typeCode," + typeCode);
        }
    }

    /**
     * Gets merge response instance by code.
     *
     * @param typeCode the type code
     * @return the merge response instance by code
     */
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
                throw new IllegalArgumentException("not support typeCode," + typeCode);
        }
    }
}
