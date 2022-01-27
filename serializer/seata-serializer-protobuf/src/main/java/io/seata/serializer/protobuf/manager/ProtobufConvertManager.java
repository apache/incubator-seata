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
package io.seata.serializer.protobuf.manager;

import io.seata.core.protocol.BatchResultMessage;
import io.seata.serializer.protobuf.convertor.BatchResultMessageConvertor;
import io.seata.serializer.protobuf.convertor.BranchCommitRequestConvertor;
import io.seata.serializer.protobuf.convertor.BranchCommitResponseConvertor;
import io.seata.serializer.protobuf.convertor.BranchRegisterRequestConvertor;
import io.seata.serializer.protobuf.convertor.BranchRegisterResponseConvertor;
import io.seata.serializer.protobuf.convertor.BranchReportRequestConvertor;
import io.seata.serializer.protobuf.convertor.BranchReportResponseConvertor;
import io.seata.serializer.protobuf.convertor.BranchRollbackRequestConvertor;
import io.seata.serializer.protobuf.convertor.BranchRollbackResponseConvertor;
import io.seata.serializer.protobuf.convertor.GlobalBeginRequestConvertor;
import io.seata.serializer.protobuf.convertor.GlobalBeginResponseConvertor;
import io.seata.serializer.protobuf.convertor.GlobalCommitRequestConvertor;
import io.seata.serializer.protobuf.convertor.GlobalCommitResponseConvertor;
import io.seata.serializer.protobuf.convertor.GlobalLockQueryRequestConvertor;
import io.seata.serializer.protobuf.convertor.GlobalLockQueryResponseConvertor;
import io.seata.serializer.protobuf.convertor.GlobalReportRequestConvertor;
import io.seata.serializer.protobuf.convertor.GlobalReportResponseConvertor;
import io.seata.serializer.protobuf.convertor.GlobalRollbackRequestConvertor;
import io.seata.serializer.protobuf.convertor.GlobalRollbackResponseConvertor;
import io.seata.serializer.protobuf.convertor.GlobalStatusRequestConvertor;
import io.seata.serializer.protobuf.convertor.GlobalStatusResponseConvertor;
import io.seata.serializer.protobuf.convertor.HeartbeatMessageConvertor;
import io.seata.serializer.protobuf.convertor.MergeResultMessageConvertor;
import io.seata.serializer.protobuf.convertor.MergedWarpMessageConvertor;
import io.seata.serializer.protobuf.convertor.PbConvertor;
import io.seata.serializer.protobuf.convertor.RegisterRMRequestConvertor;
import io.seata.serializer.protobuf.convertor.RegisterRMResponseConvertor;
import io.seata.serializer.protobuf.convertor.RegisterTMRequestConvertor;
import io.seata.serializer.protobuf.convertor.RegisterTMResponseConvertor;
import io.seata.serializer.protobuf.generated.BatchResultMessageProto;
import io.seata.serializer.protobuf.generated.GlobalReportRequestProto;
import io.seata.serializer.protobuf.generated.GlobalReportResponseProto;
import io.seata.core.protocol.HeartbeatMessage;
import io.seata.core.protocol.MergeResultMessage;
import io.seata.core.protocol.MergedWarpMessage;
import io.seata.core.protocol.RegisterRMRequest;
import io.seata.core.protocol.RegisterRMResponse;
import io.seata.core.protocol.RegisterTMRequest;
import io.seata.core.protocol.RegisterTMResponse;
import io.seata.serializer.protobuf.convertor.UndoLogDeleteRequestConvertor;
import io.seata.serializer.protobuf.generated.BranchCommitRequestProto;
import io.seata.serializer.protobuf.generated.BranchCommitResponseProto;
import io.seata.serializer.protobuf.generated.BranchRegisterRequestProto;
import io.seata.serializer.protobuf.generated.BranchRegisterResponseProto;
import io.seata.serializer.protobuf.generated.BranchReportRequestProto;
import io.seata.serializer.protobuf.generated.BranchReportResponseProto;
import io.seata.serializer.protobuf.generated.BranchRollbackRequestProto;
import io.seata.serializer.protobuf.generated.BranchRollbackResponseProto;
import io.seata.serializer.protobuf.generated.GlobalBeginRequestProto;
import io.seata.serializer.protobuf.generated.GlobalBeginResponseProto;
import io.seata.serializer.protobuf.generated.GlobalCommitRequestProto;
import io.seata.serializer.protobuf.generated.GlobalCommitResponseProto;
import io.seata.serializer.protobuf.generated.GlobalLockQueryRequestProto;
import io.seata.serializer.protobuf.generated.GlobalLockQueryResponseProto;
import io.seata.serializer.protobuf.generated.GlobalRollbackRequestProto;
import io.seata.serializer.protobuf.generated.GlobalRollbackResponseProto;
import io.seata.serializer.protobuf.generated.GlobalStatusRequestProto;
import io.seata.serializer.protobuf.generated.GlobalStatusResponseProto;
import io.seata.serializer.protobuf.generated.HeartbeatMessageProto;
import io.seata.serializer.protobuf.generated.MergedResultMessageProto;
import io.seata.serializer.protobuf.generated.MergedWarpMessageProto;
import io.seata.serializer.protobuf.generated.RegisterRMRequestProto;
import io.seata.serializer.protobuf.generated.RegisterRMResponseProto;
import io.seata.serializer.protobuf.generated.RegisterTMRequestProto;
import io.seata.serializer.protobuf.generated.RegisterTMResponseProto;
import io.seata.serializer.protobuf.generated.UndoLogDeleteRequestProto;
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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author leizhiyuan
 */
public class ProtobufConvertManager {

    private Map<String, PbConvertor> convertorMap = new ConcurrentHashMap<>();

    private Map<String, PbConvertor> reverseConvertorMap = new ConcurrentHashMap<>();

    private Map<String, Class> protoClazzMap = new ConcurrentHashMap<>();

    private static class SingletonHolder {
        private static final ProtobufConvertManager INSTANCE;

        static {
            final ProtobufConvertManager protobufConvertManager = new ProtobufConvertManager();
            protobufConvertManager.convertorMap.put(GlobalBeginRequest.class.getName(),
                new GlobalBeginRequestConvertor());
            protobufConvertManager.convertorMap.put(BranchCommitRequest.class.getName(),
                new BranchCommitRequestConvertor());
            protobufConvertManager.convertorMap.put(BranchCommitResponse.class.getName(),
                new BranchCommitResponseConvertor());
            protobufConvertManager.convertorMap.put(BranchRegisterRequest.class.getName(),
                new BranchRegisterRequestConvertor());
            protobufConvertManager.convertorMap.put(BranchRegisterResponse.class.getName(),
                new BranchRegisterResponseConvertor());
            protobufConvertManager.convertorMap.put(BranchReportRequest.class.getName(),
                new BranchReportRequestConvertor());
            protobufConvertManager.convertorMap.put(BranchReportResponse.class.getName(),
                new BranchReportResponseConvertor());
            protobufConvertManager.convertorMap.put(BranchRollbackRequest.class.getName(),
                new BranchRollbackRequestConvertor());
            protobufConvertManager.convertorMap.put(BranchRollbackResponse.class.getName(),
                new BranchRollbackResponseConvertor());
            protobufConvertManager.convertorMap.put(GlobalBeginResponse.class.getName(),
                new GlobalBeginResponseConvertor());
            protobufConvertManager.convertorMap.put(GlobalCommitRequest.class.getName(),
                new GlobalCommitRequestConvertor());
            protobufConvertManager.convertorMap.put(GlobalCommitResponse.class.getName(),
                new GlobalCommitResponseConvertor());
            protobufConvertManager.convertorMap.put(GlobalLockQueryRequest.class.getName(),
                new GlobalLockQueryRequestConvertor());
            protobufConvertManager.convertorMap.put(GlobalLockQueryResponse.class.getName(),
                new GlobalLockQueryResponseConvertor());
            protobufConvertManager.convertorMap.put(GlobalRollbackRequest.class.getName(),
                new GlobalRollbackRequestConvertor());
            protobufConvertManager.convertorMap.put(GlobalRollbackResponse.class.getName(),
                new GlobalRollbackResponseConvertor());
            protobufConvertManager.convertorMap.put(GlobalStatusRequest.class.getName(),
                new GlobalStatusRequestConvertor());
            protobufConvertManager.convertorMap.put(GlobalStatusResponse.class.getName(),
                new GlobalStatusResponseConvertor());
            protobufConvertManager.convertorMap.put(GlobalReportRequest.class.getName(),
                new GlobalReportRequestConvertor());
            protobufConvertManager.convertorMap.put(GlobalReportResponse.class.getName(),
                new GlobalReportResponseConvertor());
            protobufConvertManager.convertorMap.put(UndoLogDeleteRequest.class.getName(),
                new UndoLogDeleteRequestConvertor());

            protobufConvertManager.convertorMap.put(MergedWarpMessage.class.getName(),
                new MergedWarpMessageConvertor());
            protobufConvertManager.convertorMap.put(HeartbeatMessage.class.getName(), new HeartbeatMessageConvertor());
            protobufConvertManager.convertorMap.put(MergeResultMessage.class.getName(),
                new MergeResultMessageConvertor());
            protobufConvertManager.convertorMap.put(RegisterRMRequest.class.getName(),
                new RegisterRMRequestConvertor());
            protobufConvertManager.convertorMap.put(RegisterRMResponse.class.getName(),
                new RegisterRMResponseConvertor());
            protobufConvertManager.convertorMap.put(RegisterTMRequest.class.getName(),
                new RegisterTMRequestConvertor());
            protobufConvertManager.convertorMap.put(RegisterTMResponse.class.getName(),
                new RegisterTMResponseConvertor());
            protobufConvertManager.convertorMap.put(BatchResultMessage.class.getName(),
                new BatchResultMessageConvertor());

            protobufConvertManager.protoClazzMap.put(GlobalBeginRequestProto.getDescriptor().getFullName(),
                GlobalBeginRequestProto.class);
            protobufConvertManager.protoClazzMap.put(BranchCommitRequestProto.getDescriptor().getFullName(),
                BranchCommitRequestProto.class);
            protobufConvertManager.protoClazzMap.put(BranchCommitResponseProto.getDescriptor().getFullName(),
                BranchCommitResponseProto.class);
            protobufConvertManager.protoClazzMap.put(BranchRegisterRequestProto.getDescriptor().getFullName(),
                BranchRegisterRequestProto.class);
            protobufConvertManager.protoClazzMap.put(BranchRegisterResponseProto.getDescriptor().getFullName(),
                BranchRegisterResponseProto.class);
            protobufConvertManager.protoClazzMap.put(BranchReportRequestProto.getDescriptor().getFullName(),
                BranchReportRequestProto.class);
            protobufConvertManager.protoClazzMap.put(BranchReportResponseProto.getDescriptor().getFullName(),
                BranchReportResponseProto.class);
            protobufConvertManager.protoClazzMap.put(BranchRollbackRequestProto.getDescriptor().getFullName(),
                BranchRollbackRequestProto.class);
            protobufConvertManager.protoClazzMap.put(BranchRollbackResponseProto.getDescriptor().getFullName(),
                BranchRollbackResponseProto.class);
            protobufConvertManager.protoClazzMap.put(GlobalBeginResponseProto.getDescriptor().getFullName(),
                GlobalBeginResponseProto.class);
            protobufConvertManager.protoClazzMap.put(GlobalCommitRequestProto.getDescriptor().getFullName(),
                GlobalCommitRequestProto.class);
            protobufConvertManager.protoClazzMap.put(GlobalCommitResponseProto.getDescriptor().getFullName(),
                GlobalCommitResponseProto.class);
            protobufConvertManager.protoClazzMap.put(GlobalLockQueryRequestProto.getDescriptor().getFullName(),
                GlobalLockQueryRequestProto.class);
            protobufConvertManager.protoClazzMap.put(GlobalLockQueryResponseProto.getDescriptor().getFullName(),
                GlobalLockQueryResponseProto.class);
            protobufConvertManager.protoClazzMap.put(GlobalRollbackRequestProto.getDescriptor().getFullName(),
                GlobalRollbackRequestProto.class);
            protobufConvertManager.protoClazzMap.put(GlobalRollbackResponseProto.getDescriptor().getFullName(),
                GlobalRollbackResponseProto.class);
            protobufConvertManager.protoClazzMap.put(GlobalStatusRequestProto.getDescriptor().getFullName(),
                GlobalStatusRequestProto.class);
            protobufConvertManager.protoClazzMap.put(GlobalStatusResponseProto.getDescriptor().getFullName(),
                GlobalStatusResponseProto.class);
            protobufConvertManager.protoClazzMap.put(GlobalReportRequestProto.getDescriptor().getFullName(),
                GlobalReportRequestProto.class);
            protobufConvertManager.protoClazzMap.put(GlobalReportResponseProto.getDescriptor().getFullName(),
                GlobalReportResponseProto.class);
            protobufConvertManager.protoClazzMap.put(UndoLogDeleteRequestProto.getDescriptor().getFullName(),
                UndoLogDeleteRequestProto.class);

            protobufConvertManager.protoClazzMap.put(MergedWarpMessageProto.getDescriptor().getFullName(),
                MergedWarpMessageProto.class);
            protobufConvertManager.protoClazzMap.put(HeartbeatMessageProto.getDescriptor().getFullName(),
                HeartbeatMessageProto.class);
            protobufConvertManager.protoClazzMap.put(MergedResultMessageProto.getDescriptor().getFullName(),
                MergedResultMessageProto.class);
            protobufConvertManager.protoClazzMap.put(RegisterRMRequestProto.getDescriptor().getFullName(),
                RegisterRMRequestProto.class);
            protobufConvertManager.protoClazzMap.put(RegisterRMResponseProto.getDescriptor().getFullName(),
                RegisterRMResponseProto.class);
            protobufConvertManager.protoClazzMap.put(RegisterTMRequestProto.getDescriptor().getFullName(),
                RegisterTMRequestProto.class);
            protobufConvertManager.protoClazzMap.put(RegisterTMResponseProto.getDescriptor().getFullName(),
                RegisterTMResponseProto.class);
            protobufConvertManager.protoClazzMap.put(BatchResultMessageProto.getDescriptor().getFullName(),
                BatchResultMessageProto.class);

            protobufConvertManager.reverseConvertorMap.put(GlobalBeginRequestProto.class.getName(),
                new GlobalBeginRequestConvertor());
            protobufConvertManager.reverseConvertorMap.put(BranchCommitRequestProto.class.getName(),
                new BranchCommitRequestConvertor());
            protobufConvertManager.reverseConvertorMap.put(BranchCommitResponseProto.class.getName(),
                new BranchCommitResponseConvertor());
            protobufConvertManager.reverseConvertorMap.put(BranchRegisterRequestProto.class.getName(),
                new BranchRegisterRequestConvertor());
            protobufConvertManager.reverseConvertorMap.put(BranchRegisterResponseProto.class.getName(),
                new BranchRegisterResponseConvertor());
            protobufConvertManager.reverseConvertorMap.put(BranchReportRequestProto.class.getName(),
                new BranchReportRequestConvertor());
            protobufConvertManager.reverseConvertorMap.put(BranchReportResponseProto.class.getName(),
                new BranchReportResponseConvertor());
            protobufConvertManager.reverseConvertorMap.put(BranchRollbackRequestProto.class.getName(),
                new BranchRollbackRequestConvertor());
            protobufConvertManager.reverseConvertorMap.put(BranchRollbackResponseProto.class.getName(),
                new BranchRollbackResponseConvertor());
            protobufConvertManager.reverseConvertorMap.put(GlobalBeginResponseProto.class.getName(),
                new GlobalBeginResponseConvertor());
            protobufConvertManager.reverseConvertorMap.put(GlobalCommitRequestProto.class.getName(),
                new GlobalCommitRequestConvertor());
            protobufConvertManager.reverseConvertorMap.put(GlobalCommitResponseProto.class.getName(),
                new GlobalCommitResponseConvertor());
            protobufConvertManager.reverseConvertorMap.put(GlobalLockQueryRequestProto.class.getName(),
                new GlobalLockQueryRequestConvertor());
            protobufConvertManager.reverseConvertorMap.put(GlobalLockQueryResponseProto.class.getName(),
                new GlobalLockQueryResponseConvertor());
            protobufConvertManager.reverseConvertorMap.put(GlobalRollbackRequestProto.class.getName(),
                new GlobalRollbackRequestConvertor());
            protobufConvertManager.reverseConvertorMap.put(GlobalRollbackResponseProto.class.getName(),
                new GlobalRollbackResponseConvertor());
            protobufConvertManager.reverseConvertorMap.put(GlobalStatusRequestProto.class.getName(),
                new GlobalStatusRequestConvertor());
            protobufConvertManager.reverseConvertorMap.put(GlobalStatusResponseProto.class.getName(),
                new GlobalStatusResponseConvertor());
            protobufConvertManager.reverseConvertorMap.put(GlobalReportRequestProto.class.getName(),
                new GlobalReportRequestConvertor());
            protobufConvertManager.reverseConvertorMap.put(GlobalReportResponseProto.class.getName(),
                new GlobalReportResponseConvertor());
            protobufConvertManager.reverseConvertorMap.put(UndoLogDeleteRequestProto.class.getName(),
                new UndoLogDeleteRequestConvertor());

            protobufConvertManager.reverseConvertorMap.put(MergedWarpMessageProto.class.getName(),
                new MergedWarpMessageConvertor());
            protobufConvertManager.reverseConvertorMap.put(HeartbeatMessageProto.class.getName(),
                new HeartbeatMessageConvertor());
            protobufConvertManager.reverseConvertorMap.put(MergedResultMessageProto.class.getName(),
                new MergeResultMessageConvertor());
            protobufConvertManager.reverseConvertorMap.put(RegisterRMRequestProto.class.getName(),
                new RegisterRMRequestConvertor());
            protobufConvertManager.reverseConvertorMap.put(RegisterRMResponseProto.class.getName(),
                new RegisterRMResponseConvertor());
            protobufConvertManager.reverseConvertorMap.put(RegisterTMRequestProto.class.getName(),
                new RegisterTMRequestConvertor());
            protobufConvertManager.reverseConvertorMap.put(RegisterTMResponseProto.class.getName(),
                new RegisterTMResponseConvertor());
            protobufConvertManager.reverseConvertorMap.put(BatchResultMessageProto.class.getName(),
                new BatchResultMessageConvertor());

            INSTANCE = protobufConvertManager;
        }

    }

    /**
     * Gets instance.
     *
     * @return the instance
     */
    public static final ProtobufConvertManager getInstance() {
        return SingletonHolder.INSTANCE;
    }

    public PbConvertor fetchConvertor(String clazz) {
        return convertorMap.get(clazz);
    }

    public PbConvertor fetchReversedConvertor(String clazz) {
        return reverseConvertorMap.get(clazz);
    }

    public Class fetchProtoClass(String clazz) {
        return protoClazzMap.get(clazz);
    }

}