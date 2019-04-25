/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2019 All Rights Reserved.
 */
package io.seata.core.protocol.serialize;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.seata.core.protocol.HeartbeatMessage;
import io.seata.core.protocol.MergeResultMessage;
import io.seata.core.protocol.MergedWarpMessage;
import io.seata.core.protocol.RegisterRMRequest;
import io.seata.core.protocol.RegisterRMResponse;
import io.seata.core.protocol.RegisterTMRequest;
import io.seata.core.protocol.RegisterTMResponse;
import io.seata.core.protocol.convertor.BranchCommitRequestConvertor;
import io.seata.core.protocol.convertor.BranchCommitResponseConvertor;
import io.seata.core.protocol.convertor.BranchRegisterRequestConvertor;
import io.seata.core.protocol.convertor.BranchRegisterResponseConvertor;
import io.seata.core.protocol.convertor.BranchReportRequestConvertor;
import io.seata.core.protocol.convertor.BranchReportResponseConvertor;
import io.seata.core.protocol.convertor.BranchRollbackRequestConvertor;
import io.seata.core.protocol.convertor.BranchRollbackResponseConvertor;
import io.seata.core.protocol.convertor.GlobalBeginRequestConvertor;
import io.seata.core.protocol.convertor.GlobalBeginResponseConvertor;
import io.seata.core.protocol.convertor.GlobalCommitRequestConvertor;
import io.seata.core.protocol.convertor.GlobalCommitResponseConvertor;
import io.seata.core.protocol.convertor.GlobalLockQueryRequestConvertor;
import io.seata.core.protocol.convertor.GlobalLockQueryResponseConvertor;
import io.seata.core.protocol.convertor.GlobalRollbackRequestConvertor;
import io.seata.core.protocol.convertor.GlobalRollbackResponseConvertor;
import io.seata.core.protocol.convertor.GlobalStatusRequestConvertor;
import io.seata.core.protocol.convertor.GlobalStatusResponseConvertor;
import io.seata.core.protocol.convertor.HeartbeatMessageConvertor;
import io.seata.core.protocol.convertor.MergeResultMessageConvertor;
import io.seata.core.protocol.convertor.MergedWarpMessageConvertor;
import io.seata.core.protocol.convertor.PbConvertor;
import io.seata.core.protocol.convertor.RegisterRMRequestConvertor;
import io.seata.core.protocol.convertor.RegisterRMResponseConvertor;
import io.seata.core.protocol.convertor.RegisterTMRequestConvertor;
import io.seata.core.protocol.convertor.RegisterTMResponseConvertor;
import io.seata.core.protocol.protobuf.BranchCommitRequestProto;
import io.seata.core.protocol.protobuf.BranchCommitResponseProto;
import io.seata.core.protocol.protobuf.BranchRegisterRequestProto;
import io.seata.core.protocol.protobuf.BranchRegisterResponseProto;
import io.seata.core.protocol.protobuf.BranchReportRequestProto;
import io.seata.core.protocol.protobuf.BranchReportResponseProto;
import io.seata.core.protocol.protobuf.BranchRollbackRequestProto;
import io.seata.core.protocol.protobuf.BranchRollbackResponseProto;
import io.seata.core.protocol.protobuf.GlobalBeginRequestProto;
import io.seata.core.protocol.protobuf.GlobalBeginResponseProto;
import io.seata.core.protocol.protobuf.GlobalCommitRequestProto;
import io.seata.core.protocol.protobuf.GlobalCommitResponseProto;
import io.seata.core.protocol.protobuf.GlobalLockQueryRequestProto;
import io.seata.core.protocol.protobuf.GlobalLockQueryResponseProto;
import io.seata.core.protocol.protobuf.GlobalRollbackRequestProto;
import io.seata.core.protocol.protobuf.GlobalRollbackResponseProto;
import io.seata.core.protocol.protobuf.GlobalStatusRequestProto;
import io.seata.core.protocol.protobuf.GlobalStatusResponseProto;
import io.seata.core.protocol.protobuf.HeartbeatMessageProto;
import io.seata.core.protocol.protobuf.MergedResultMessageProto;
import io.seata.core.protocol.protobuf.MergedWarpMessageProto;
import io.seata.core.protocol.protobuf.RegisterRMRequestProto;
import io.seata.core.protocol.protobuf.RegisterRMResponseProto;
import io.seata.core.protocol.protobuf.RegisterTMRequestProto;
import io.seata.core.protocol.protobuf.RegisterTMResponseProto;
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
import io.seata.core.protocol.transaction.GlobalRollbackRequest;
import io.seata.core.protocol.transaction.GlobalRollbackResponse;
import io.seata.core.protocol.transaction.GlobalStatusRequest;
import io.seata.core.protocol.transaction.GlobalStatusResponse;

/**
 * @author bystander
 * @version : ProtobufConvertManager.java, v 0.1 2019年04月25日 07:41 bystander Exp $
 */
public class ProtobufConvertManager {

    private Map<String, PbConvertor> convertorMap = new ConcurrentHashMap<>();

    private Map<String, Class> clazzMap = new ConcurrentHashMap<>();

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

            protobufConvertManager.clazzMap.put(GlobalBeginRequestProto.class.getName(), GlobalBeginRequestProto.class);
            protobufConvertManager.clazzMap.put(BranchCommitRequestProto.class.getName(),
                BranchCommitRequestProto.class);
            protobufConvertManager.clazzMap.put(BranchCommitResponseProto.class.getName(),
                BranchCommitResponseProto.class);
            protobufConvertManager.clazzMap.put(BranchRegisterRequestProto.class.getName(),
                BranchRegisterRequestProto.class);
            protobufConvertManager.clazzMap.put(BranchRegisterResponseProto.class.getName(),
                BranchRegisterResponseProto.class);
            protobufConvertManager.clazzMap.put(BranchReportRequestProto.class.getName(),
                BranchReportRequestProto.class);
            protobufConvertManager.clazzMap.put(BranchReportResponseProto.class.getName(),
                BranchReportResponseProto.class);
            protobufConvertManager.clazzMap.put(BranchRollbackRequestProto.class.getName(),
                BranchRollbackRequestProto.class);
            protobufConvertManager.clazzMap.put(BranchRollbackResponseProto.class.getName(),
                BranchRollbackResponseProto.class);
            protobufConvertManager.clazzMap.put(GlobalBeginResponseProto.class.getName(),
                GlobalBeginResponseProto.class);
            protobufConvertManager.clazzMap.put(GlobalCommitRequestProto.class.getName(),
                GlobalCommitRequestProto.class);
            protobufConvertManager.clazzMap.put(GlobalCommitResponseProto.class.getName(),
                GlobalCommitResponseProto.class);
            protobufConvertManager.clazzMap.put(GlobalLockQueryRequestProto.class.getName(),
                GlobalLockQueryRequestProto.class);
            protobufConvertManager.clazzMap.put(GlobalLockQueryResponseProto.class.getName(),
                GlobalLockQueryResponseProto.class);
            protobufConvertManager.clazzMap.put(GlobalRollbackRequestProto.class.getName(),
                GlobalRollbackRequestProto.class);
            protobufConvertManager.clazzMap.put(GlobalRollbackResponseProto.class.getName(),
                GlobalRollbackResponseProto.class);
            protobufConvertManager.clazzMap.put(GlobalStatusRequestProto.class.getName(),
                GlobalStatusRequestProto.class);
            protobufConvertManager.clazzMap.put(GlobalStatusResponseProto.class.getName(),
                GlobalStatusResponseProto.class);

            protobufConvertManager.clazzMap.put(MergedWarpMessageProto.class.getName(), MergedWarpMessageProto.class);
            protobufConvertManager.clazzMap.put(HeartbeatMessageProto.class.getName(), HeartbeatMessageProto.class);
            protobufConvertManager.clazzMap.put(MergedResultMessageProto.class.getName(), MergedResultMessageProto.class);
            protobufConvertManager.clazzMap.put(RegisterRMRequestProto.class.getName(), RegisterRMRequestProto.class);
            protobufConvertManager.clazzMap.put(RegisterRMResponseProto.class.getName(), RegisterRMResponseProto.class);
            protobufConvertManager.clazzMap.put(RegisterTMRequestProto.class.getName(), RegisterTMRequestProto.class);
            protobufConvertManager.clazzMap.put(RegisterTMResponseProto.class.getName(), RegisterTMResponseProto.class);

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

    public PbConvertor fetcConvertor(String clazz) {
        return convertorMap.get(clazz);
    }

    public Class fetchClass(String clazz) {
        return clazzMap.get(clazz);
    }

}