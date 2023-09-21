package io.seata.serializer.seata;

import io.seata.core.protocol.AbstractMessage;
import io.seata.core.protocol.BatchResultMessage;
import io.seata.core.protocol.MergeResultMessage;
import io.seata.core.protocol.MergedWarpMessage;
import io.seata.core.protocol.MessageType;
import io.seata.core.protocol.RegisterRMRequest;
import io.seata.core.protocol.RegisterRMResponse;
import io.seata.core.protocol.RegisterTMRequest;
import io.seata.core.protocol.RegisterTMResponse;
import io.seata.core.protocol.transaction.BranchCommitRequest;
import io.seata.core.protocol.transaction.BranchCommitResponse;
import io.seata.core.protocol.transaction.BranchRegisterRequest;
import io.seata.core.protocol.transaction.BranchRegisterResponse;
import io.seata.core.protocol.transaction.BranchReportRequest;
import io.seata.core.protocol.transaction.BranchReportResponse;
import io.seata.core.protocol.transaction.BranchRollbackRequest;
import io.seata.core.protocol.transaction.BranchRollbackResponse;
import io.seata.core.protocol.transaction.GlobalBeginRequest;
import io.seata.core.protocol.transaction.GlobalBeginResponse;
import io.seata.core.protocol.transaction.GlobalCommitRequest;
import io.seata.core.protocol.transaction.GlobalCommitResponse;
import io.seata.core.protocol.transaction.GlobalLockQueryRequest;
import io.seata.core.protocol.transaction.GlobalLockQueryResponse;
import io.seata.core.protocol.transaction.GlobalReportRequest;
import io.seata.core.protocol.transaction.GlobalReportResponse;
import io.seata.core.protocol.transaction.GlobalRollbackRequest;
import io.seata.core.protocol.transaction.GlobalRollbackResponse;
import io.seata.core.protocol.transaction.GlobalStatusRequest;
import io.seata.core.protocol.transaction.GlobalStatusResponse;
import io.seata.core.protocol.transaction.UndoLogDeleteRequest;

public abstract class MessageCodecFactory {

    public abstract MessageSeataCodec getMessageCodec(short typeCode);


    /**
     * Get message codec message codec.
     *
     * @param abstractMessage the abstract message
     * @return the message codec
     */
    public MessageSeataCodec getMessageCodec(AbstractMessage abstractMessage) {
        return getMessageCodec(abstractMessage.getTypeCode());
    }

    /**
     * Gets message.
     *
     * @param typeCode the type code
     * @return the message
     */
    public AbstractMessage getMessage(short typeCode) {
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
            default:
                break;
        }

        if (abstractMessage != null) {
            return abstractMessage;
        }

        try {
            abstractMessage = getMergeRequestInstanceByCode(typeCode);
        } catch (Exception exx) {
        }

        if (abstractMessage != null) {
            return abstractMessage;
        }

        return getMergeResponseInstanceByCode(typeCode);
    }

    /**
     * Gets merge request instance by code.
     *
     * @param typeCode the type code
     * @return the merge request instance by code
     */
    protected static AbstractMessage getMergeRequestInstanceByCode(int typeCode) {
        switch (typeCode) {
            case MessageType.TYPE_GLOBAL_BEGIN:
                return new GlobalBeginRequest();
            case MessageType.TYPE_GLOBAL_COMMIT:
                return new GlobalCommitRequest();
            case MessageType.TYPE_GLOBAL_ROLLBACK:
                return new GlobalRollbackRequest();
            case MessageType.TYPE_GLOBAL_STATUS:
                return new GlobalStatusRequest();
            case MessageType.TYPE_GLOBAL_LOCK_QUERY:
                return new GlobalLockQueryRequest();
            case MessageType.TYPE_BRANCH_REGISTER:
                return new BranchRegisterRequest();
            case MessageType.TYPE_BRANCH_STATUS_REPORT:
                return new BranchReportRequest();
            case MessageType.TYPE_GLOBAL_REPORT:
                return new GlobalReportRequest();
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
    protected static AbstractMessage getMergeResponseInstanceByCode(int typeCode) {
        switch (typeCode) {
            case MessageType.TYPE_GLOBAL_BEGIN_RESULT:
                return new GlobalBeginResponse();
            case MessageType.TYPE_GLOBAL_COMMIT_RESULT:
                return new GlobalCommitResponse();
            case MessageType.TYPE_GLOBAL_ROLLBACK_RESULT:
                return new GlobalRollbackResponse();
            case MessageType.TYPE_GLOBAL_STATUS_RESULT:
                return new GlobalStatusResponse();
            case MessageType.TYPE_GLOBAL_LOCK_QUERY_RESULT:
                return new GlobalLockQueryResponse();
            case MessageType.TYPE_BRANCH_REGISTER_RESULT:
                return new BranchRegisterResponse();
            case MessageType.TYPE_BRANCH_STATUS_REPORT_RESULT:
                return new BranchReportResponse();
            case MessageType.TYPE_BRANCH_COMMIT_RESULT:
                return new BranchCommitResponse();
            case MessageType.TYPE_BRANCH_ROLLBACK_RESULT:
                return new BranchRollbackResponse();
            case MessageType.TYPE_GLOBAL_REPORT_RESULT:
                return new GlobalReportResponse();
            default:
                throw new IllegalArgumentException("not support typeCode," + typeCode);
        }
    }
}
