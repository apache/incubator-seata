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
package io.seata.core.rpc.grpc.service;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import io.seata.core.protocol.RegisterRMResponse;
import io.seata.core.protocol.transaction.BranchRegisterRequest;
import io.seata.core.protocol.transaction.BranchRegisterResponse;
import io.seata.core.protocol.transaction.BranchReportRequest;
import io.seata.core.protocol.transaction.BranchReportResponse;
import io.seata.core.protocol.transaction.GlobalLockQueryRequest;
import io.seata.core.protocol.transaction.GlobalLockQueryResponse;
import io.seata.core.rpc.SeataChannel;
import io.seata.core.rpc.grpc.BiStreamMessageTypeHelper;
import io.seata.core.rpc.grpc.GrpcRemotingServer;
import io.seata.core.rpc.grpc.GrpcSeataChannel;
import io.seata.core.rpc.grpc.ProtoTypeConvertHelper;
import io.seata.core.rpc.grpc.generated.GrpcRemoting;
import io.seata.core.rpc.grpc.generated.ResourceManagerServiceGrpc;
import io.seata.core.rpc.processor.MessageMeta;
import io.seata.core.rpc.processor.RpcMessageHandleContext;
import io.seata.serializer.protobuf.generated.BranchRegisterRequestProto;
import io.seata.serializer.protobuf.generated.BranchRegisterResponseProto;
import io.seata.serializer.protobuf.generated.BranchReportRequestProto;
import io.seata.serializer.protobuf.generated.BranchReportResponseProto;
import io.seata.serializer.protobuf.generated.GlobalLockQueryRequestProto;
import io.seata.serializer.protobuf.generated.GlobalLockQueryResponseProto;
import io.seata.serializer.protobuf.generated.RegisterRMRequestProto;
import io.seata.serializer.protobuf.generated.RegisterRMResponseProto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.seata.core.rpc.grpc.ContextKeyConstants.CUR_CONNECTION;
import static io.seata.core.rpc.grpc.ContextKeyConstants.CUR_CONNECT_ID;

/**
 * @author goodboycoder
 */
public class ResourceManagerService extends ResourceManagerServiceGrpc.ResourceManagerServiceImplBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceManagerService.class);

    private final GrpcRemotingServer remotingServer;

    public ResourceManagerService(GrpcRemotingServer remotingServer) {
        this.remotingServer = remotingServer;
    }

    @Override
    public StreamObserver<GrpcRemoting.BiStreamMessage> registerRM(StreamObserver<GrpcRemoting.BiStreamMessage> responseObserver) {
        return new StreamObserver<GrpcRemoting.BiStreamMessage>() {
            @Override
            public void onNext(GrpcRemoting.BiStreamMessage biStreamMessage) {
                GrpcRemoting.BiStreamMessageType messageType = biStreamMessage.getMessageType();
                Any message = biStreamMessage.getMessage();

                Object requestModel;
                RpcMessageHandleContext handleContext;
                if (GrpcRemoting.BiStreamMessageType.TYPERegisterRMRequest == messageType) {
                    RegisterRMRequestProto registerRMRequestProto;
                    try {
                        registerRMRequestProto = message.unpack(RegisterRMRequestProto.class);
                    } catch (InvalidProtocolBufferException e) {
                        throw new RuntimeException(e);
                    }
                    requestModel = ProtoTypeConvertHelper.convertToModel(registerRMRequestProto);
                    handleContext = buildHandleContext(responseObserver);
                    handleContext.setMessageReply(response -> {
                        if (!(response instanceof RegisterRMResponse)) {
                            LOGGER.warn("[GRPC]wrong response type, need {} but actually {}", RegisterRMResponse.class, response.getClass());
                            return;
                        }
                        RegisterRMResponseProto responseProto = (RegisterRMResponseProto) ProtoTypeConvertHelper.convertToProto(response);
                        GrpcRemoting.BiStreamMessage responseMessage = GrpcRemoting.BiStreamMessage.newBuilder()
                                .setID(biStreamMessage.getID())
                                .setMessageType(GrpcRemoting.BiStreamMessageType.TYPERegisterRMResponse)
                                .setMessage(Any.pack(responseProto))
                                .setClientId(handleContext.channel().getId())
                                .build();
                        try {
                            responseObserver.onNext(responseMessage);
                        } catch (Exception e) {
                            LOGGER.warn("[GRPC]fail to send response, req:{}, resp:{}", registerRMRequestProto, response);
                        }
                    });
                    remotingServer.processMessage(handleContext, requestModel);
                } else if (isProcessable(messageType)) {
                    Message unpackMessage;
                    try {
                        unpackMessage = message.unpack(BiStreamMessageTypeHelper.getBiStreamMessageClassType(messageType));
                    } catch (InvalidProtocolBufferException e) {
                        throw new RuntimeException(e);
                    }
                    requestModel = ProtoTypeConvertHelper.convertToModel(unpackMessage);
                    handleContext = buildHandleContext(responseObserver, biStreamMessage);
                    remotingServer.processMessage(handleContext, requestModel);
                } else {
                    LOGGER.warn("unprocessable message: {}", biStreamMessage);
                }
            }

            @Override
            public void onError(Throwable throwable) {
                LOGGER.error("RM Bi stream on error, error: {}, will close the responseObserver", throwable.toString());
                if (responseObserver instanceof ServerCallStreamObserver) {
                    ServerCallStreamObserver<?> serverCallStreamObserver = (ServerCallStreamObserver<?>) responseObserver;
                    if (!serverCallStreamObserver.isCancelled()) {
                        serverCallStreamObserver.onCompleted();
                    }
                } else {
                    responseObserver.onCompleted();
                }
            }

            @Override
            public void onCompleted() {
                LOGGER.info("RM Bi stream on completed");
                if (responseObserver instanceof ServerCallStreamObserver) {
                    ServerCallStreamObserver<?> serverCallStreamObserver = (ServerCallStreamObserver<?>) responseObserver;
                    if (!serverCallStreamObserver.isCancelled()) {
                        serverCallStreamObserver.onCompleted();
                    }
                } else {
                    responseObserver.onCompleted();
                }
            }
        };
    }

    @Override
    public void branchRegister(BranchRegisterRequestProto request, StreamObserver<BranchRegisterResponseProto> responseObserver) {
        BranchRegisterRequest requestModel = (BranchRegisterRequest) ProtoTypeConvertHelper.convertToModel(request);
        RpcMessageHandleContext handleContext = buildHandleContext(responseObserver);
        handleContext.setMessageReply(response -> {
            if (!(response instanceof BranchRegisterResponse)) {
                LOGGER.warn("[GRPC]wrong response type, need {} but actually {}", BranchRegisterResponse.class, response.getClass());
                return;
            }
            try {
                responseObserver.onNext((BranchRegisterResponseProto) ProtoTypeConvertHelper.convertToProto(response));
                responseObserver.onCompleted();
            } catch (Exception e) {
                LOGGER.warn("[GRPC]fail to send response, req:{}, resp:{}", request, response);
            }
        });
        remotingServer.processMessage(handleContext, requestModel);
    }

    @Override
    public void branchReport(BranchReportRequestProto request, StreamObserver<BranchReportResponseProto> responseObserver) {
        BranchReportRequest requestModel = (BranchReportRequest) ProtoTypeConvertHelper.convertToModel(request);
        RpcMessageHandleContext handleContext = buildHandleContext(responseObserver);
        handleContext.setMessageReply(response -> {
            if (!(response instanceof BranchReportResponse)) {
                LOGGER.warn("[GRPC]wrong response type, need {} but actually {}", BranchReportResponse.class, response.getClass());
                return;
            }
            try {
                responseObserver.onNext((BranchReportResponseProto) ProtoTypeConvertHelper.convertToProto(response));
                responseObserver.onCompleted();
            } catch (Exception e) {
                LOGGER.warn("[GRPC]fail to send response, req:{}, resp:{}", request, response);
            }
        });
        remotingServer.processMessage(handleContext, requestModel);
    }

    @Override
    public void lockQuery(GlobalLockQueryRequestProto request, StreamObserver<GlobalLockQueryResponseProto> responseObserver) {
        GlobalLockQueryRequest requestModel = (GlobalLockQueryRequest) ProtoTypeConvertHelper.convertToModel(request);
        RpcMessageHandleContext handleContext = buildHandleContext(responseObserver);
        handleContext.setMessageReply(response -> {
            if (!(response instanceof GlobalLockQueryResponse)) {
                LOGGER.warn("[GRPC]wrong response type, need {} but actually {}", GlobalLockQueryResponse.class, response.getClass());
                return;
            }
            try {
                responseObserver.onNext((GlobalLockQueryResponseProto) ProtoTypeConvertHelper.convertToProto(response));
                responseObserver.onCompleted();
            } catch (Exception e) {
                LOGGER.warn("[GRPC]fail to send response, req:{}, resp:{}", request, response);
            }
        });
        remotingServer.processMessage(handleContext, requestModel);
    }

    public RpcMessageHandleContext buildHandleContext(StreamObserver<? extends Message> responseObserver, GrpcRemoting.BiStreamMessage message) {
        SeataChannel curChannel = new GrpcSeataChannel(CUR_CONNECT_ID.get(), CUR_CONNECTION.get(), responseObserver);
        RpcMessageHandleContext ctx = new RpcMessageHandleContext(curChannel);
        if (null != message) {
            MessageMeta messageMeta = new MessageMeta();
            messageMeta.setMessageId(message.getID());
            ctx.setMessageMeta(messageMeta);
        }
        return ctx;
    }
    public RpcMessageHandleContext buildHandleContext(StreamObserver<? extends Message> responseObserver) {
        return buildHandleContext(responseObserver, null);
    }

    private boolean isProcessable(GrpcRemoting.BiStreamMessageType messageType) {
        return GrpcRemoting.BiStreamMessageType.TypeBranchCommitResult == messageType
                || GrpcRemoting.BiStreamMessageType.TypeBranchRollBackResult == messageType;
    }
}
