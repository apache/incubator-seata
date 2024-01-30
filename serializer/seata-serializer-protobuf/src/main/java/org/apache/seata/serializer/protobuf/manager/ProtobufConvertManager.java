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
package org.apache.seata.serializer.protobuf.manager;

import org.apache.seata.core.protocol.BatchResultMessage;
import org.apache.seata.serializer.protobuf.convertor.BatchResultMessageConvertor;
import org.apache.seata.serializer.protobuf.convertor.BranchCommitRequestConvertor;
import org.apache.seata.serializer.protobuf.convertor.BranchCommitResponseConvertor;
import org.apache.seata.serializer.protobuf.convertor.BranchRegisterRequestConvertor;
import org.apache.seata.serializer.protobuf.convertor.BranchRegisterResponseConvertor;
import org.apache.seata.serializer.protobuf.convertor.BranchReportRequestConvertor;
import org.apache.seata.serializer.protobuf.convertor.BranchReportResponseConvertor;
import org.apache.seata.serializer.protobuf.convertor.BranchRollbackRequestConvertor;
import org.apache.seata.serializer.protobuf.convertor.BranchRollbackResponseConvertor;
import org.apache.seata.serializer.protobuf.convertor.GlobalBeginRequestConvertor;
import org.apache.seata.serializer.protobuf.convertor.GlobalBeginResponseConvertor;
import org.apache.seata.serializer.protobuf.convertor.GlobalCommitRequestConvertor;
import org.apache.seata.serializer.protobuf.convertor.GlobalCommitResponseConvertor;
import org.apache.seata.serializer.protobuf.convertor.GlobalLockQueryRequestConvertor;
import org.apache.seata.serializer.protobuf.convertor.GlobalLockQueryResponseConvertor;
import org.apache.seata.serializer.protobuf.convertor.GlobalReportRequestConvertor;
import org.apache.seata.serializer.protobuf.convertor.GlobalReportResponseConvertor;
import org.apache.seata.serializer.protobuf.convertor.GlobalRollbackRequestConvertor;
import org.apache.seata.serializer.protobuf.convertor.GlobalRollbackResponseConvertor;
import org.apache.seata.serializer.protobuf.convertor.GlobalStatusRequestConvertor;
import org.apache.seata.serializer.protobuf.convertor.GlobalStatusResponseConvertor;
import org.apache.seata.serializer.protobuf.convertor.HeartbeatMessageConvertor;
import org.apache.seata.serializer.protobuf.convertor.MergeResultMessageConvertor;
import org.apache.seata.serializer.protobuf.convertor.MergedWarpMessageConvertor;
import org.apache.seata.serializer.protobuf.convertor.PbConvertor;
import org.apache.seata.serializer.protobuf.convertor.RegisterRMRequestConvertor;
import org.apache.seata.serializer.protobuf.convertor.RegisterRMResponseConvertor;
import org.apache.seata.serializer.protobuf.convertor.RegisterTMRequestConvertor;
import org.apache.seata.serializer.protobuf.convertor.RegisterTMResponseConvertor;
import org.apache.seata.serializer.protobuf.generated.BatchResultMessageProto;
import org.apache.seata.serializer.protobuf.generated.GlobalReportRequestProto;
import org.apache.seata.serializer.protobuf.generated.GlobalReportResponseProto;
import org.apache.seata.core.protocol.HeartbeatMessage;
import org.apache.seata.core.protocol.MergeResultMessage;
import org.apache.seata.core.protocol.MergedWarpMessage;
import org.apache.seata.core.protocol.RegisterRMRequest;
import org.apache.seata.core.protocol.RegisterRMResponse;
import org.apache.seata.core.protocol.RegisterTMRequest;
import org.apache.seata.core.protocol.RegisterTMResponse;
import org.apache.seata.serializer.protobuf.convertor.UndoLogDeleteRequestConvertor;
import org.apache.seata.serializer.protobuf.generated.BranchCommitRequestProto;
import org.apache.seata.serializer.protobuf.generated.BranchCommitResponseProto;
import org.apache.seata.serializer.protobuf.generated.BranchRegisterRequestProto;
import org.apache.seata.serializer.protobuf.generated.BranchRegisterResponseProto;
import org.apache.seata.serializer.protobuf.generated.BranchReportRequestProto;
import org.apache.seata.serializer.protobuf.generated.BranchReportResponseProto;
import org.apache.seata.serializer.protobuf.generated.BranchRollbackRequestProto;
import org.apache.seata.serializer.protobuf.generated.BranchRollbackResponseProto;
import org.apache.seata.serializer.protobuf.generated.GlobalBeginRequestProto;
import org.apache.seata.serializer.protobuf.generated.GlobalBeginResponseProto;
import org.apache.seata.serializer.protobuf.generated.GlobalCommitRequestProto;
import org.apache.seata.serializer.protobuf.generated.GlobalCommitResponseProto;
import org.apache.seata.serializer.protobuf.generated.GlobalLockQueryRequestProto;
import org.apache.seata.serializer.protobuf.generated.GlobalLockQueryResponseProto;
import org.apache.seata.serializer.protobuf.generated.GlobalRollbackRequestProto;
import org.apache.seata.serializer.protobuf.generated.GlobalRollbackResponseProto;
import org.apache.seata.serializer.protobuf.generated.GlobalStatusRequestProto;
import org.apache.seata.serializer.protobuf.generated.GlobalStatusResponseProto;
import org.apache.seata.serializer.protobuf.generated.HeartbeatMessageProto;
import org.apache.seata.serializer.protobuf.generated.MergedResultMessageProto;
import org.apache.seata.serializer.protobuf.generated.MergedWarpMessageProto;
import org.apache.seata.serializer.protobuf.generated.RegisterRMRequestProto;
import org.apache.seata.serializer.protobuf.generated.RegisterRMResponseProto;
import org.apache.seata.serializer.protobuf.generated.RegisterTMRequestProto;
import org.apache.seata.serializer.protobuf.generated.RegisterTMResponseProto;
import org.apache.seata.serializer.protobuf.generated.UndoLogDeleteRequestProto;
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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


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
