/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.serializer.seata;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.apache.seata.serializer.seata.protocol.BatchResultMessageCodec;
import org.apache.seata.serializer.seata.protocol.MergeResultMessageCodec;
import org.apache.seata.serializer.seata.protocol.MergedWarpMessageCodec;
import org.apache.seata.serializer.seata.protocol.RegisterRMRequestCodec;
import org.apache.seata.serializer.seata.protocol.RegisterRMResponseCodec;
import org.apache.seata.serializer.seata.protocol.RegisterTMRequestCodec;
import org.apache.seata.serializer.seata.protocol.RegisterTMResponseCodec;
import org.apache.seata.serializer.seata.protocol.transaction.BranchCommitRequestCodec;
import org.apache.seata.serializer.seata.protocol.transaction.BranchCommitResponseCodec;
import org.apache.seata.serializer.seata.protocol.transaction.BranchRegisterRequestCodec;
import org.apache.seata.serializer.seata.protocol.transaction.BranchRegisterResponseCodec;
import org.apache.seata.serializer.seata.protocol.transaction.BranchReportRequestCodec;
import org.apache.seata.serializer.seata.protocol.transaction.BranchReportResponseCodec;
import org.apache.seata.serializer.seata.protocol.transaction.BranchRollbackRequestCodec;
import org.apache.seata.serializer.seata.protocol.transaction.BranchRollbackResponseCodec;
import org.apache.seata.serializer.seata.protocol.transaction.GlobalBeginRequestCodec;
import org.apache.seata.serializer.seata.protocol.transaction.GlobalBeginResponseCodec;
import org.apache.seata.serializer.seata.protocol.transaction.GlobalCommitRequestCodec;
import org.apache.seata.serializer.seata.protocol.transaction.GlobalCommitResponseCodec;
import org.apache.seata.serializer.seata.protocol.transaction.GlobalLockQueryRequestCodec;
import org.apache.seata.serializer.seata.protocol.transaction.GlobalLockQueryResponseCodec;
import org.apache.seata.serializer.seata.protocol.transaction.GlobalReportRequestCodec;
import org.apache.seata.serializer.seata.protocol.transaction.GlobalReportResponseCodec;
import org.apache.seata.serializer.seata.protocol.transaction.GlobalRollbackRequestCodec;
import org.apache.seata.serializer.seata.protocol.transaction.GlobalRollbackResponseCodec;
import org.apache.seata.serializer.seata.protocol.transaction.GlobalStatusRequestCodec;
import org.apache.seata.serializer.seata.protocol.transaction.GlobalStatusResponseCodec;
import org.apache.seata.serializer.seata.protocol.transaction.UndoLogDeleteRequestCodec;
import org.apache.seata.core.protocol.AbstractMessage;
import org.apache.seata.core.protocol.BatchResultMessage;
import org.apache.seata.core.protocol.MergeResultMessage;
import org.apache.seata.core.protocol.MergedWarpMessage;
import org.apache.seata.core.protocol.MessageType;
import org.apache.seata.core.protocol.RegisterRMRequest;
import org.apache.seata.core.protocol.RegisterRMResponse;
import org.apache.seata.core.protocol.RegisterTMRequest;
import org.apache.seata.core.protocol.RegisterTMResponse;
import org.apache.seata.core.protocol.transaction.BranchCommitRequest;
import org.apache.seata.core.protocol.transaction.BranchCommitResponse;
import org.apache.seata.core.protocol.transaction.BranchRegisterRequest;
import org.apache.seata.core.protocol.transaction.BranchRegisterResponse;
import org.apache.seata.core.protocol.transaction.BranchReportRequest;
import org.apache.seata.core.protocol.transaction.BranchReportResponse;
import org.apache.seata.core.protocol.transaction.BranchRollbackRequest;
import org.apache.seata.core.protocol.transaction.BranchRollbackResponse;
import org.apache.seata.core.protocol.transaction.GlobalBeginRequest;
import org.apache.seata.core.protocol.transaction.GlobalBeginResponse;
import org.apache.seata.core.protocol.transaction.GlobalCommitRequest;
import org.apache.seata.core.protocol.transaction.GlobalCommitResponse;
import org.apache.seata.core.protocol.transaction.GlobalLockQueryRequest;
import org.apache.seata.core.protocol.transaction.GlobalLockQueryResponse;
import org.apache.seata.core.protocol.transaction.GlobalReportRequest;
import org.apache.seata.core.protocol.transaction.GlobalReportResponse;
import org.apache.seata.core.protocol.transaction.GlobalRollbackRequest;
import org.apache.seata.core.protocol.transaction.GlobalRollbackResponse;
import org.apache.seata.core.protocol.transaction.GlobalStatusRequest;
import org.apache.seata.core.protocol.transaction.GlobalStatusResponse;
import org.apache.seata.core.protocol.transaction.UndoLogDeleteRequest;

/**
 * The type Message codec factory.
 */
public class MessageCodecFactory {

    /**
     * The constant UTF8.
     */
    protected static final Charset UTF8 = StandardCharsets.UTF_8;

    /**
     * Get message codec message codec.
     *
     * @param abstractMessage the abstract message
     * @return the message codec
     */
    public static MessageSeataCodec getMessageCodec(AbstractMessage abstractMessage, byte version) {
        return getMessageCodec(abstractMessage.getTypeCode(), version);
    }

    /**
     * Gets msg instance by code.
     *
     * @param typeCode the type code
     * @return the msg instance by code
     */
    public static MessageSeataCodec getMessageCodec(short typeCode, byte version) {
        MessageSeataCodec msgCodec = null;
        switch (typeCode) {
            case MessageType.TYPE_SEATA_MERGE:
                msgCodec = new MergedWarpMessageCodec(version);
                break;
            case MessageType.TYPE_SEATA_MERGE_RESULT:
                msgCodec = new MergeResultMessageCodec(version);
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
                msgCodec = new BatchResultMessageCodec(version);
                break;
            case MessageType.TYPE_GLOBAL_BEGIN:
                msgCodec = new GlobalBeginRequestCodec();
                break;
            case MessageType.TYPE_GLOBAL_COMMIT:
                msgCodec = new GlobalCommitRequestCodec();
                break;
            case MessageType.TYPE_GLOBAL_ROLLBACK:
                msgCodec = new GlobalRollbackRequestCodec();
                break;
            case MessageType.TYPE_GLOBAL_STATUS:
                msgCodec = new GlobalStatusRequestCodec();
                break;
            case MessageType.TYPE_GLOBAL_LOCK_QUERY:
                msgCodec = new GlobalLockQueryRequestCodec();
                break;
            case MessageType.TYPE_BRANCH_REGISTER:
                msgCodec = new BranchRegisterRequestCodec();
                break;
            case MessageType.TYPE_BRANCH_STATUS_REPORT:
                msgCodec = new BranchReportRequestCodec();
                break;
            case MessageType.TYPE_GLOBAL_BEGIN_RESULT:
                msgCodec = new GlobalBeginResponseCodec();
                break;
            case MessageType.TYPE_GLOBAL_COMMIT_RESULT:
                msgCodec = new GlobalCommitResponseCodec();
                break;
            case MessageType.TYPE_GLOBAL_ROLLBACK_RESULT:
                msgCodec = new GlobalRollbackResponseCodec();
                break;
            case MessageType.TYPE_GLOBAL_STATUS_RESULT:
                msgCodec = new GlobalStatusResponseCodec();
                break;
            case MessageType.TYPE_GLOBAL_LOCK_QUERY_RESULT:
                msgCodec = new GlobalLockQueryResponseCodec();
                break;
            case MessageType.TYPE_BRANCH_REGISTER_RESULT:
                msgCodec = new BranchRegisterResponseCodec();
                break;
            case MessageType.TYPE_BRANCH_STATUS_REPORT_RESULT:
                msgCodec = new BranchReportResponseCodec();
                break;
            case MessageType.TYPE_BRANCH_COMMIT_RESULT:
                msgCodec = new BranchCommitResponseCodec();
                break;
            case MessageType.TYPE_BRANCH_ROLLBACK_RESULT:
                msgCodec = new BranchRollbackResponseCodec();
                break;
            case MessageType.TYPE_RM_DELETE_UNDOLOG:
                msgCodec = new UndoLogDeleteRequestCodec();
                break;
            case MessageType.TYPE_GLOBAL_REPORT_RESULT:
                msgCodec = new GlobalReportResponseCodec();
                break;
            default:
                break;
        }

        if (msgCodec != null) {
            return msgCodec;
        }

        throw new IllegalArgumentException("not support typeCode," + typeCode);
    }

    /**
     * Gets message.
     *
     * @param typeCode the type code
     * @return the message
     */
    public static AbstractMessage getMessage(short typeCode) {
        AbstractMessage abstractMessage = null;
        switch (typeCode) {
            case MessageType.TYPE_SEATA_MERGE:
                abstractMessage = new MergedWarpMessage();
                break;
            case MessageType.TYPE_SEATA_MERGE_RESULT:
                abstractMessage = new MergeResultMessage();
                break;
            case MessageType.TYPE_REG_CLT:
                abstractMessage = new RegisterTMRequest();
                break;
            case MessageType.TYPE_REG_CLT_RESULT:
                abstractMessage = new RegisterTMResponse();
                break;
            case MessageType.TYPE_REG_RM:
                abstractMessage = new RegisterRMRequest();
                break;
            case MessageType.TYPE_REG_RM_RESULT:
                abstractMessage = new RegisterRMResponse();
                break;
            case MessageType.TYPE_BRANCH_COMMIT:
                abstractMessage = new BranchCommitRequest();
                break;
            case MessageType.TYPE_BRANCH_ROLLBACK:
                abstractMessage = new BranchRollbackRequest();
                break;
            case MessageType.TYPE_RM_DELETE_UNDOLOG:
                abstractMessage = new UndoLogDeleteRequest();
                break;
            case MessageType.TYPE_GLOBAL_REPORT:
                abstractMessage = new GlobalReportRequest();
                break;
            case MessageType.TYPE_GLOBAL_REPORT_RESULT:
                abstractMessage = new GlobalReportResponse();
                break;
            case MessageType.TYPE_BATCH_RESULT_MSG:
                abstractMessage = new BatchResultMessage();
                break;
            case MessageType.TYPE_GLOBAL_BEGIN:
                abstractMessage = new GlobalBeginRequest();
                break;
            case MessageType.TYPE_GLOBAL_COMMIT:
                abstractMessage = new GlobalCommitRequest();
                break;
            case MessageType.TYPE_GLOBAL_ROLLBACK:
                abstractMessage = new GlobalRollbackRequest();
                break;
            case MessageType.TYPE_GLOBAL_STATUS:
                abstractMessage = new GlobalStatusRequest();
                break;
            case MessageType.TYPE_GLOBAL_LOCK_QUERY:
                abstractMessage = new GlobalLockQueryRequest();
                break;
            case MessageType.TYPE_BRANCH_REGISTER:
                abstractMessage = new BranchRegisterRequest();
                break;
            case MessageType.TYPE_BRANCH_STATUS_REPORT:
                abstractMessage = new BranchReportRequest();
                break;
            case MessageType.TYPE_GLOBAL_BEGIN_RESULT:
                abstractMessage = new GlobalBeginResponse();
                break;
            case MessageType.TYPE_GLOBAL_COMMIT_RESULT:
                abstractMessage = new GlobalCommitResponse();
                break;
            case MessageType.TYPE_GLOBAL_ROLLBACK_RESULT:
                abstractMessage = new GlobalRollbackResponse();
                break;
            case MessageType.TYPE_GLOBAL_STATUS_RESULT:
                abstractMessage = new GlobalStatusResponse();
                break;
            case MessageType.TYPE_GLOBAL_LOCK_QUERY_RESULT:
                abstractMessage = new GlobalLockQueryResponse();
                break;
            case MessageType.TYPE_BRANCH_REGISTER_RESULT:
                abstractMessage = new BranchRegisterResponse();
                break;
            case MessageType.TYPE_BRANCH_STATUS_REPORT_RESULT:
                abstractMessage = new BranchReportResponse();
                break;
            case MessageType.TYPE_BRANCH_COMMIT_RESULT:
                abstractMessage = new BranchCommitResponse();
                break;
            case MessageType.TYPE_BRANCH_ROLLBACK_RESULT:
                abstractMessage = new BranchRollbackResponse();
                break;
            default:
                break;
        }

        if (abstractMessage != null) {
            return abstractMessage;
        }

        throw new IllegalArgumentException("not support typeCode," + typeCode);
    }

}
