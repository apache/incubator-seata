/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2019 All Rights Reserved.
 */
package io.seata.core.protocol.serialize;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
import io.seata.core.protocol.transaction.GlobalBeginRequest;
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


import io.seata.core.protocol.MergedWarpMessage;
import io.seata.core.protocol.HeartbeatMessage;
import io.seata.core.protocol.MergeResultMessage;
import io.seata.core.protocol.RegisterRMRequest;
import io.seata.core.protocol.RegisterRMResponse;
import io.seata.core.protocol.RegisterTMRequest;
import io.seata.core.protocol.RegisterTMResponse;

/**
 * @author bystander
 * @version : ProtobufConvertManager.java, v 0.1 2019年04月25日 07:41 bystander Exp $
 */
public class ProtobufConvertManager {

    private Map<String, PbConvertor> convertorMap = new ConcurrentHashMap<>();

    private static class SingletonHolder {
        private static final ProtobufConvertManager INSTANCE;

        static {
            final ProtobufConvertManager protobufConvertManager = new ProtobufConvertManager();
            protobufConvertManager.convertorMap.put(GlobalBeginRequest.class.getName(), new GlobalBeginRequestConvertor());
            protobufConvertManager.convertorMap.put(BranchCommitRequest.class.getName(),new BranchCommitRequestConvertor());
            protobufConvertManager.convertorMap.put(BranchCommitResponse.class.getName(),new BranchCommitResponseConvertor());
            protobufConvertManager.convertorMap.put(BranchRegisterRequest.class.getName(),new BranchRegisterRequestConvertor());
            protobufConvertManager.convertorMap.put(BranchRegisterResponse.class.getName(),new BranchRegisterResponseConvertor());
            protobufConvertManager.convertorMap.put(BranchReportRequest.class.getName(),new BranchReportRequestConvertor());
            protobufConvertManager.convertorMap.put(BranchReportResponse.class.getName(),new BranchReportResponseConvertor());
            protobufConvertManager.convertorMap.put(BranchRollbackRequest.class.getName(),new BranchRollbackRequestConvertor());
            protobufConvertManager.convertorMap.put(BranchRollbackResponse.class.getName(),new BranchRollbackResponseConvertor());
            protobufConvertManager.convertorMap.put(GlobalBeginResponse.class.getName(),new GlobalBeginResponseConvertor());
            protobufConvertManager.convertorMap.put(GlobalCommitRequest.class.getName(),new GlobalCommitRequestConvertor());
            protobufConvertManager.convertorMap.put(GlobalCommitResponse.class.getName(),new GlobalCommitResponseConvertor());
            protobufConvertManager.convertorMap.put(GlobalLockQueryRequest.class.getName(),new GlobalLockQueryRequestConvertor());
            protobufConvertManager.convertorMap.put(GlobalLockQueryResponse.class.getName(),new GlobalLockQueryResponseConvertor());
            protobufConvertManager.convertorMap.put(GlobalRollbackRequest.class.getName(),new GlobalRollbackRequestConvertor());
            protobufConvertManager.convertorMap.put(GlobalRollbackResponse.class.getName(),new GlobalRollbackResponseConvertor());
            protobufConvertManager.convertorMap.put(GlobalStatusRequest.class.getName(),new GlobalStatusRequestConvertor());
            protobufConvertManager.convertorMap.put(GlobalStatusResponse.class.getName(),new GlobalStatusResponseConvertor());


            protobufConvertManager.convertorMap.put(MergedWarpMessage.class.getName(),new MergedWarpMessageConvertor());
            protobufConvertManager.convertorMap.put(HeartbeatMessage.class.getName(),new HeartbeatMessageConvertor());
            protobufConvertManager.convertorMap.put(MergeResultMessage.class.getName(),new MergeResultMessageConvertor());
            protobufConvertManager.convertorMap.put(RegisterRMRequest.class.getName(),new RegisterRMRequestConvertor());
            protobufConvertManager.convertorMap.put(RegisterRMResponse.class.getName(),new RegisterRMResponseConvertor());
            protobufConvertManager.convertorMap.put(RegisterTMRequest.class.getName(),new RegisterTMRequestConvertor());
            protobufConvertManager.convertorMap.put(RegisterTMResponse.class.getName(),new RegisterTMResponseConvertor());


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
}