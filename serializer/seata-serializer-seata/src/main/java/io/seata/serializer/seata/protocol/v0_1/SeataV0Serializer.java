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
package io.seata.serializer.seata.protocol.v0_1;

import io.seata.core.protocol.MessageType;
import io.seata.core.rpc.netty.v0.MessageCodecV0;
import io.seata.core.rpc.netty.v0.SerializerV0;

/**
 * The Seata codec v0.
 *
 * @author Bughue
 */
public class SeataV0Serializer implements SerializerV0 {

    public SeataV0Serializer() {

    }


    @Override
    public MessageCodecV0 getMsgInstanceByCode(short typeCode) {
        MessageCodecV0 msgCodec = null;
        switch (typeCode) {
//            case MessageType.TYPE_SEATA_MERGE:
//                msgCodec = new MergedWarpMessage();
//                break;
//            case MessageType.TYPE_SEATA_MERGE_RESULT:
//                msgCodec = new MergeResultMessage();
//                break;
            case MessageType.TYPE_REG_CLT:
                msgCodec = new RegisterTMRequestV0();
                break;
            case MessageType.TYPE_REG_CLT_RESULT:
                msgCodec = new RegisterTMResponseV0();
                break;
            case MessageType.TYPE_REG_RM:
                msgCodec = new RegisterRMRequestV0();
                break;
            case MessageType.TYPE_REG_RM_RESULT:
                msgCodec = new RegisterRMResponseV0();
                break;
//            case MessageType.TYPE_BRANCH_COMMIT:
//                msgCodec = new BranchCommitRequest();
//                break;
//            case MessageType.TYPE_BRANCH_ROLLBACK:
//                msgCodec = new BranchRollbackRequest();
//                break;
            default:
                break;
        }

        if (null != msgCodec) {
            return msgCodec;
        }

        try {
            msgCodec = (MessageCodecV0) getMergeRequestInstanceByCode(typeCode);
        } catch (Exception exx) {

        }
        if (null != msgCodec) {
            return msgCodec;
        }

        return (MessageCodecV0)getMergeResponseInstanceByCode(typeCode);
    }

    /**
     * Gets merge request instance by code.
     *
     * @param typeCode the type code
     * @return the merge request instance by code
     */
    public static MergedMessageV0 getMergeRequestInstanceByCode(int typeCode) {
        switch (typeCode) {
//            case MessageType.TYPE_GLOBAL_BEGIN:
//                return new GlobalBeginRequest();
//            case MessageType.TYPE_GLOBAL_COMMIT:
//                return new GlobalCommitRequest();
//            case MessageType.TYPE_GLOBAL_ROLLBACK:
//                return new GlobalRollbackRequest();
//            case MessageType.TYPE_GLOBAL_STATUS:
//                return new GlobalStatusRequest();
//            case MessageType.TYPE_GLOBAL_LOCK_QUERY:
//                return new GlobalLockQueryRequest();
//            case MessageType.TYPE_BRANCH_REGISTER:
//                return new BranchRegisterRequest();
//            case MessageType.TYPE_BRANCH_STATUS_REPORT:
//                return new BranchReportRequest();
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
    public static MergedMessageV0 getMergeResponseInstanceByCode(int typeCode) {
        switch (typeCode) {
//            case MessageType.TYPE_GLOBAL_BEGIN_RESULT:
//                return new GlobalBeginResponse();
//            case MessageType.TYPE_GLOBAL_COMMIT_RESULT:
//                return new GlobalCommitResponse();
//            case MessageType.TYPE_GLOBAL_ROLLBACK_RESULT:
//                return new GlobalRollbackResponse();
//            case MessageType.TYPE_GLOBAL_STATUS_RESULT:
//                return new GlobalStatusResponse();
//            case MessageType.TYPE_GLOBAL_LOCK_QUERY_RESULT:
//                return new GlobalLockQueryResponse();
//            case MessageType.TYPE_BRANCH_REGISTER_RESULT:
//                return new BranchRegisterResponse();
//            case MessageType.TYPE_BRANCH_STATUS_REPORT_RESULT:
//                return new BranchReportResponse();
//            case MessageType.TYPE_BRANCH_COMMIT_RESULT:
//                return new BranchCommitResponse();
//            case MessageType.TYPE_BRANCH_ROLLBACK_RESULT:
//                return new BranchRollbackResponse();
            default:
                throw new IllegalArgumentException("not support typeCode," + typeCode);
        }
    }
}
