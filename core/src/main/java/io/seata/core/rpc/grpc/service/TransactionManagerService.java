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
import io.seata.core.protocol.RegisterTMResponse;
import io.seata.core.protocol.transaction.GlobalBeginRequest;
import io.seata.core.protocol.transaction.GlobalBeginResponse;
import io.seata.core.protocol.transaction.GlobalCommitRequest;
import io.seata.core.protocol.transaction.GlobalCommitResponse;
import io.seata.core.protocol.transaction.GlobalReportRequest;
import io.seata.core.protocol.transaction.GlobalReportResponse;
import io.seata.core.protocol.transaction.GlobalRollbackRequest;
import io.seata.core.protocol.transaction.GlobalRollbackResponse;
import io.seata.core.protocol.transaction.GlobalStatusRequest;
import io.seata.core.protocol.transaction.GlobalStatusResponse;
import io.seata.core.rpc.SeataChannel;
import io.seata.core.rpc.grpc.BiStreamMessageTypeHelper;
import io.seata.core.rpc.grpc.GrpcRemotingServer;
import io.seata.core.rpc.grpc.GrpcSeataChannel;
import io.seata.core.rpc.grpc.ProtoTypeConvertHelper;
import io.seata.core.rpc.grpc.generated.GrpcRemoting;
import io.seata.core.rpc.grpc.generated.TransactionManagerServiceGrpc;
import io.seata.core.rpc.processor.MessageMeta;
import io.seata.core.rpc.processor.RpcMessageHandleContext;
import io.seata.serializer.protobuf.generated.GlobalBeginRequestProto;
import io.seata.serializer.protobuf.generated.GlobalBeginResponseProto;
import io.seata.serializer.protobuf.generated.GlobalCommitRequestProto;
import io.seata.serializer.protobuf.generated.GlobalCommitResponseProto;
import io.seata.serializer.protobuf.generated.GlobalReportRequestProto;
import io.seata.serializer.protobuf.generated.GlobalReportResponseProto;
import io.seata.serializer.protobuf.generated.GlobalRollbackRequestProto;
import io.seata.serializer.protobuf.generated.GlobalRollbackResponseProto;
import io.seata.serializer.protobuf.generated.GlobalStatusRequestProto;
import io.seata.serializer.protobuf.generated.GlobalStatusResponseProto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.seata.core.rpc.grpc.ContextKeyConstants.CUR_CONNECTION;
import static io.seata.core.rpc.grpc.ContextKeyConstants.CUR_CONNECT_ID;

/**
 * @author goodboycoder
 */
public class TransactionManagerService extends TransactionManagerServiceGrpc.TransactionManagerServiceImplBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionManagerService.class);

    private final GrpcRemotingServer remotingServer;

    public TransactionManagerService(GrpcRemotingServer remotingServer) {
        this.remotingServer = remotingServer;
    }

    @Override
    public StreamObserver<GrpcRemoting.BiStreamMessage> registerTM(StreamObserver<GrpcRemoting.BiStreamMessage> responseObserver) {
        return new StreamObserver<GrpcRemoting.BiStreamMessage>() {
            @Override
            public void onNext(GrpcRemoting.BiStreamMessage biStreamMessage) {
                GrpcRemoting.BiStreamMessageType messageType = biStreamMessage.getMessageType();
                Any message = biStreamMessage.getMessage();
                Object requestModel;
                RpcMessageHandleContext handleContext;
                if (GrpcRemoting.BiStreamMessageType.TYPERegisterTMRequest == messageType) {
                    io.seata.serializer.protobuf.generated.RegisterTMRequestProto registerTMRequestProto;
                    try {
                        registerTMRequestProto = message.unpack(io.seata.serializer.protobuf.generated.RegisterTMRequestProto.class);
                    } catch (InvalidProtocolBufferException e) {
                        throw new RuntimeException(e);
                    }
                    requestModel = ProtoTypeConvertHelper.convertToModel(registerTMRequestProto);
                    handleContext = buildHandleContext(responseObserver);
                    handleContext.setMessageReply(response -> {
                        if (!(response instanceof RegisterTMResponse)) {
                            LOGGER.warn("[GRPC]wrong response type, need {} but actually {}", RegisterTMResponse.class, response.getClass());
                            return;
                        }
                        io.seata.serializer.protobuf.generated.RegisterTMResponseProto responseProto = (io.seata.serializer.protobuf.generated.RegisterTMResponseProto) ProtoTypeConvertHelper.convertToProto(response);
                        GrpcRemoting.BiStreamMessage responseMessage = GrpcRemoting.BiStreamMessage.newBuilder()
                                .setID(biStreamMessage.getID())
                                .setMessageType(GrpcRemoting.BiStreamMessageType.TYPERegisterTMResponse)
                                .setMessage(Any.pack(responseProto))
                                .setClientId(handleContext.channel().getId())
                                .build();
                        try {
                            responseObserver.onNext(responseMessage);
                        } catch (Exception e) {
                            LOGGER.warn("[GRPC]fail to send response, req:{}, resp:{}", registerTMRequestProto, response);
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
                LOGGER.error("TM Bi stream on error, error: {}, will close the responseObserver", throwable.toString());
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
                LOGGER.info("TM Bi stream on completed");
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
    public void globalBegin(GlobalBeginRequestProto request, StreamObserver<GlobalBeginResponseProto> responseObserver) {
        GlobalBeginRequest requestModel = (GlobalBeginRequest) ProtoTypeConvertHelper.convertToModel(request);
        RpcMessageHandleContext handleContext = buildHandleContext(responseObserver);
        handleContext.setMessageReply(response -> {
            if (!(response instanceof GlobalBeginResponse)) {
                LOGGER.warn("[GRPC]wrong response type, need {} but actually {}", GlobalBeginResponse.class, response.getClass());
                return;
            }
            try {
                responseObserver.onNext((GlobalBeginResponseProto) ProtoTypeConvertHelper.convertToProto(response));
                responseObserver.onCompleted();
            } catch (Exception e) {
                LOGGER.warn("[GRPC]fail to send response, req:{}, resp:{}", request, response);
            }
        });
        remotingServer.processMessage(handleContext, requestModel);
    }

    @Override
    public void globalCommit(GlobalCommitRequestProto request, StreamObserver<GlobalCommitResponseProto> responseObserver) {
        GlobalCommitRequest requestModel = (GlobalCommitRequest) ProtoTypeConvertHelper.convertToModel(request);
        RpcMessageHandleContext handleContext = buildHandleContext(responseObserver);
        handleContext.setMessageReply(response -> {
            if (!(response instanceof GlobalCommitResponse)) {
                LOGGER.warn("[GRPC]wrong response type, need {} but actually {}", GlobalCommitResponse.class, response.getClass());
                return;
            }
            try {
                responseObserver.onNext((GlobalCommitResponseProto) ProtoTypeConvertHelper.convertToProto(response));
                responseObserver.onCompleted();
            } catch (Exception e) {
                LOGGER.warn("[GRPC]fail to send response, req:{}, resp:{}", request, response);
            }
        });
        remotingServer.processMessage(handleContext, requestModel);
    }

    @Override
    public void globalRollback(GlobalRollbackRequestProto request, StreamObserver<GlobalRollbackResponseProto> responseObserver) {
        GlobalRollbackRequest requestModel = (GlobalRollbackRequest) ProtoTypeConvertHelper.convertToModel(request);
        RpcMessageHandleContext handleContext = buildHandleContext(responseObserver);
        handleContext.setMessageReply(response -> {
            if (!(response instanceof GlobalRollbackResponse)) {
                LOGGER.warn("[GRPC]wrong response type, need {} but actually {}", GlobalRollbackResponse.class, response.getClass());
                return;
            }
            try {
                responseObserver.onNext((GlobalRollbackResponseProto) ProtoTypeConvertHelper.convertToProto(response));
                responseObserver.onCompleted();
            } catch (Exception e) {
                LOGGER.warn("[GRPC]fail to send response, req:{}, resp:{}", request, response);
            }
        });
        remotingServer.processMessage(handleContext, requestModel);
    }

    @Override
    public void globalReport(GlobalReportRequestProto request, StreamObserver<GlobalReportResponseProto> responseObserver) {
        GlobalReportRequest requestModel = (GlobalReportRequest) ProtoTypeConvertHelper.convertToModel(request);
        RpcMessageHandleContext handleContext = buildHandleContext(responseObserver);
        handleContext.setMessageReply(response -> {
            if (!(response instanceof GlobalReportResponse)) {
                LOGGER.warn("[GRPC]wrong response type, need {} but actually {}", GlobalReportResponse.class, response.getClass());
                return;
            }
            try {
                responseObserver.onNext((GlobalReportResponseProto) ProtoTypeConvertHelper.convertToProto(response));
                responseObserver.onCompleted();
            } catch (Exception e) {
                LOGGER.warn("[GRPC]fail to send response, req:{}, resp:{}", request, response);
            }
        });
        remotingServer.processMessage(handleContext, requestModel);
    }

    @Override
    public void getGlobalStatus(GlobalStatusRequestProto request, StreamObserver<GlobalStatusResponseProto> responseObserver) {
        GlobalStatusRequest requestModel = (GlobalStatusRequest) ProtoTypeConvertHelper.convertToModel(request);
        RpcMessageHandleContext handleContext = buildHandleContext(responseObserver);
        handleContext.setMessageReply(response -> {
            if (!(response instanceof GlobalStatusResponse)) {
                LOGGER.warn("[GRPC]wrong response type, need {} but actually {}", GlobalStatusResponse.class, response.getClass());
                return;
            }
            try {
                responseObserver.onNext((GlobalStatusResponseProto) ProtoTypeConvertHelper.convertToProto(response));
                responseObserver.onCompleted();
            } catch (Exception e) {
                LOGGER.warn("[GRPC]fail to send response, req:{}, resp:{}", request, response);
            }
        });
        remotingServer.processMessage(handleContext, requestModel);
    }

    public RpcMessageHandleContext buildHandleContext(StreamObserver<? extends Message> responseObserver, GrpcRemoting.BiStreamMessage message) {
        RpcMessageHandleContext ctx = buildHandleContext(responseObserver);
        if (null != message) {
            MessageMeta messageMeta = new MessageMeta();
            messageMeta.setMessageId(message.getID());
            ctx.setMessageMeta(messageMeta);
        }
        return ctx;
    }

    public RpcMessageHandleContext buildHandleContext(StreamObserver<? extends Message> responseObserver) {
        SeataChannel curChannel = new GrpcSeataChannel(CUR_CONNECT_ID.get(), CUR_CONNECTION.get(), responseObserver);
        return new RpcMessageHandleContext(curChannel);
    }

    private boolean isProcessable(GrpcRemoting.BiStreamMessageType messageType) {
        //This method is used to set the processable message type from TM to TC.
        //Since there is no related message at present, it is set to false by default as reserved,
        //that is, no message is processed.
        return false;
    }
}
