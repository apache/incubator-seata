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
package io.seata.serializer.seata.protocol.v1;

import io.seata.core.protocol.MessageType;
import io.seata.serializer.seata.MessageSeataCodec;
import io.seata.serializer.seata.MessageCodecFactory;
import io.seata.serializer.seata.protocol.v1.transaction.BranchCommitRequestCodec;
import io.seata.serializer.seata.protocol.v1.transaction.BranchCommitResponseCodec;
import io.seata.serializer.seata.protocol.v1.transaction.BranchRegisterRequestCodec;
import io.seata.serializer.seata.protocol.v1.transaction.BranchRegisterResponseCodec;
import io.seata.serializer.seata.protocol.v1.transaction.BranchReportRequestCodec;
import io.seata.serializer.seata.protocol.v1.transaction.BranchReportResponseCodec;
import io.seata.serializer.seata.protocol.v1.transaction.BranchRollbackRequestCodec;
import io.seata.serializer.seata.protocol.v1.transaction.BranchRollbackResponseCodec;
import io.seata.serializer.seata.protocol.v1.transaction.GlobalBeginRequestCodec;
import io.seata.serializer.seata.protocol.v1.transaction.GlobalBeginResponseCodec;
import io.seata.serializer.seata.protocol.v1.transaction.GlobalCommitRequestCodec;
import io.seata.serializer.seata.protocol.v1.transaction.GlobalCommitResponseCodec;
import io.seata.serializer.seata.protocol.v1.transaction.GlobalLockQueryRequestCodec;
import io.seata.serializer.seata.protocol.v1.transaction.GlobalLockQueryResponseCodec;
import io.seata.serializer.seata.protocol.v1.transaction.GlobalReportRequestCodec;
import io.seata.serializer.seata.protocol.v1.transaction.GlobalReportResponseCodec;
import io.seata.serializer.seata.protocol.v1.transaction.GlobalRollbackRequestCodec;
import io.seata.serializer.seata.protocol.v1.transaction.GlobalRollbackResponseCodec;
import io.seata.serializer.seata.protocol.v1.transaction.GlobalStatusRequestCodec;
import io.seata.serializer.seata.protocol.v1.transaction.GlobalStatusResponseCodec;
import io.seata.serializer.seata.protocol.v1.transaction.UndoLogDeleteRequestCodec;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * The type Message codec factory.
 *
 * @author zhangsen
 */
public class MessageCodecFactoryV1 extends MessageCodecFactory {
    /**
     * The constant UTF8.
     */
    protected static final Charset UTF8 = StandardCharsets.UTF_8;

    /**
     * Gets msg instance by code.
     *
     * @param typeCode the type code
     * @return the msg instance by code
     */
    public MessageSeataCodec getMessageCodec(short typeCode) {
        MessageSeataCodec msgCodec = null;
        switch (typeCode) {
            case MessageType.TYPE_SEATA_MERGE:
                msgCodec = new MergedWarpMessageCodec();
                break;
            case MessageType.TYPE_SEATA_MERGE_RESULT:
                msgCodec = new MergeResultMessageCodec();
                break;
            case MessageType.TYPE_REG_CLT:
                msgCodec = new RegisterTMRequestCodec();
                break;
            case MessageType.TYPE_REG_CLT_RESULT:
                msgCodec = new RegisterTMResponseCodec();
                break;
            case MessageType.TYPE_REG_RM:
                msgCodec = new RegisterRMRequestCodec();
                break;
            case MessageType.TYPE_REG_RM_RESULT:
                msgCodec = new RegisterRMResponseCodec();
                break;
            case MessageType.TYPE_BRANCH_COMMIT:
                msgCodec = new BranchCommitRequestCodec();
                break;
            case MessageType.TYPE_BRANCH_ROLLBACK:
                msgCodec = new BranchRollbackRequestCodec();
                break;
            case MessageType.TYPE_GLOBAL_REPORT:
                msgCodec = new GlobalReportRequestCodec();
                break;
            case MessageType.TYPE_BATCH_RESULT_MSG:
                msgCodec = new BatchResultMessageCodec();
                break;
            default:
                break;
        }

        if (msgCodec != null) {
            return msgCodec;
        }

        try {
            msgCodec = getMergeRequestMessageSeataCodec(typeCode);
        } catch (Exception exx) {
        }

        if (msgCodec != null) {
            return msgCodec;
        }

        msgCodec = getMergeResponseMessageSeataCodec(typeCode);

        return msgCodec;
    }

    /**
     * Gets merge request instance by code.
     *
     * @param typeCode the type code
     * @return the merge request instance by code
     */
    protected static MessageSeataCodec getMergeRequestMessageSeataCodec(int typeCode) {
        switch (typeCode) {
            case MessageType.TYPE_GLOBAL_BEGIN:
                return new GlobalBeginRequestCodec();
            case MessageType.TYPE_GLOBAL_COMMIT:
                return new GlobalCommitRequestCodec();
            case MessageType.TYPE_GLOBAL_ROLLBACK:
                return new GlobalRollbackRequestCodec();
            case MessageType.TYPE_GLOBAL_STATUS:
                return new GlobalStatusRequestCodec();
            case MessageType.TYPE_GLOBAL_LOCK_QUERY:
                return new GlobalLockQueryRequestCodec();
            case MessageType.TYPE_BRANCH_REGISTER:
                return new BranchRegisterRequestCodec();
            case MessageType.TYPE_BRANCH_STATUS_REPORT:
                return new BranchReportRequestCodec();
            case MessageType.TYPE_GLOBAL_REPORT:
                return new GlobalReportRequestCodec();
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
    public static MessageSeataCodec getMergeResponseMessageSeataCodec(int typeCode) {
        switch (typeCode) {
            case MessageType.TYPE_GLOBAL_BEGIN_RESULT:
                return new GlobalBeginResponseCodec();
            case MessageType.TYPE_GLOBAL_COMMIT_RESULT:
                return new GlobalCommitResponseCodec();
            case MessageType.TYPE_GLOBAL_ROLLBACK_RESULT:
                return new GlobalRollbackResponseCodec();
            case MessageType.TYPE_GLOBAL_STATUS_RESULT:
                return new GlobalStatusResponseCodec();
            case MessageType.TYPE_GLOBAL_LOCK_QUERY_RESULT:
                return new GlobalLockQueryResponseCodec();
            case MessageType.TYPE_BRANCH_REGISTER_RESULT:
                return new BranchRegisterResponseCodec();
            case MessageType.TYPE_BRANCH_STATUS_REPORT_RESULT:
                return new BranchReportResponseCodec();
            case MessageType.TYPE_BRANCH_COMMIT_RESULT:
                return new BranchCommitResponseCodec();
            case MessageType.TYPE_BRANCH_ROLLBACK_RESULT:
                return new BranchRollbackResponseCodec();
            case MessageType.TYPE_RM_DELETE_UNDOLOG:
                return new UndoLogDeleteRequestCodec();
            case MessageType.TYPE_GLOBAL_REPORT_RESULT:
                return new GlobalReportResponseCodec();
            default:
                throw new IllegalArgumentException("not support typeCode," + typeCode);
        }
    }



}
