package io.seata.core.rpc.grpc.service;

import java.util.HashMap;
import java.util.Map;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import io.grpc.stub.StreamObserver;
import io.seata.core.protocol.RegisterRMResponse;
import io.seata.core.protocol.transaction.BranchRegisterRequest;
import io.seata.core.protocol.transaction.BranchRegisterResponse;
import io.seata.core.protocol.transaction.BranchReportRequest;
import io.seata.core.protocol.transaction.BranchReportResponse;
import io.seata.core.protocol.transaction.GlobalLockQueryRequest;
import io.seata.core.protocol.transaction.GlobalLockQueryResponse;
import io.seata.core.rpc.SeataChannel;
import io.seata.core.rpc.grpc.GrpcRemotingServer;
import io.seata.core.rpc.grpc.GrpcSeataChannel;
import io.seata.core.rpc.grpc.generated.GrpcRemoting;
import io.seata.core.rpc.grpc.generated.ResourceManagerServiceGrpc;
import io.seata.core.rpc.processor.RpcMessageHandleContext;
import io.seata.serializer.protobuf.convertor.PbConvertor;
import io.seata.serializer.protobuf.generated.BranchRegisterRequestProto;
import io.seata.serializer.protobuf.generated.BranchRegisterResponseProto;
import io.seata.serializer.protobuf.generated.BranchReportRequestProto;
import io.seata.serializer.protobuf.generated.BranchReportResponseProto;
import io.seata.serializer.protobuf.generated.GlobalLockQueryRequestProto;
import io.seata.serializer.protobuf.generated.GlobalLockQueryResponseProto;
import io.seata.serializer.protobuf.generated.RegisterRMRequestProto;
import io.seata.serializer.protobuf.generated.RegisterRMResponseProto;
import io.seata.serializer.protobuf.manager.ProtobufConvertManager;
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

    @SuppressWarnings("unchecked")
    @Override
    public StreamObserver<GrpcRemoting.BiStreamMessage> registerRM(StreamObserver<GrpcRemoting.BiStreamMessage> responseObserver) {
        return new StreamObserver<GrpcRemoting.BiStreamMessage>() {
            @Override
            public void onNext(GrpcRemoting.BiStreamMessage biStreamMessage) {
                //获取请求类型
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
                    final PbConvertor pbConvertor = ProtobufConvertManager.getInstance().fetchConvertor(registerRMRequestProto.getClass().getName());
                    requestModel = pbConvertor.convert2Model(registerRMRequestProto);
                    handleContext = buildHandleContext(responseObserver);
                    handleContext.setMessageReply(response -> {
                        if (!(response instanceof RegisterRMResponse)) {
                            LOGGER.warn("[GRPC]wrong response type, need {} but actually {}", RegisterRMResponse.class, response.getClass());
                            return;
                        }
                        PbConvertor convertor = ProtobufConvertManager.getInstance().fetchConvertor(RegisterRMResponse.class.getName());
                        RegisterRMResponseProto responseProto = (RegisterRMResponseProto) convertor.convert2Proto(response);
                        GrpcRemoting.BiStreamMessage responseMessage = GrpcRemoting.BiStreamMessage.newBuilder().setID(biStreamMessage.getID()).setMessageType(GrpcRemoting.BiStreamMessageType.TYPERegisterRMResponse).setMessage(Any.pack(responseProto)).build();
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
                        unpackMessage = message.unpack(MessageTypeHelper.getBiStreamMessageClassType(messageType));
                    } catch (InvalidProtocolBufferException e) {
                        throw new RuntimeException(e);
                    }
                    final PbConvertor pbConvertor = ProtobufConvertManager.getInstance().fetchConvertor(unpackMessage.getClass().getName());
                    requestModel = pbConvertor.convert2Model(unpackMessage);
                    handleContext = buildHandleContext(responseObserver);
                    remotingServer.processMessage(handleContext, requestModel);
                } else {
                    LOGGER.warn("unprocessable message: {}", biStreamMessage);
                }
            }

            @Override
            public void onError(Throwable throwable) {
                LOGGER.error("RM Bi stream on error, error: {}", throwable.toString());
                //unregister channel
            }

            @Override
            public void onCompleted() {
                LOGGER.info("RM Bi stream on completed");
                //unregister channel
            }
        };
    }

    @SuppressWarnings("unchecked")
    @Override
    public void branchRegister(BranchRegisterRequestProto request, StreamObserver<BranchRegisterResponseProto> responseObserver) {
        final PbConvertor pbConvertor = ProtobufConvertManager.getInstance().fetchConvertor(request.getClass().getName());
        BranchRegisterRequest requestModel = (BranchRegisterRequest) pbConvertor.convert2Model(request);
        RpcMessageHandleContext handleContext = buildHandleContext(responseObserver);
        handleContext.setMessageReply(response -> {
            if (!(response instanceof BranchRegisterResponse)) {
                LOGGER.warn("[GRPC]wrong response type, need {} but actually {}", BranchRegisterResponse.class, response.getClass());
                return;
            }
            PbConvertor convertor = ProtobufConvertManager.getInstance().fetchConvertor(BranchRegisterResponse.class.getName());
            try {
                responseObserver.onNext((BranchRegisterResponseProto) convertor.convert2Proto(response));
                responseObserver.onCompleted();
            } catch (Exception e) {
                LOGGER.warn("[GRPC]fail to send response, req:{}, resp:{}", request, response);
            }
        });
        remotingServer.processMessage(handleContext, requestModel);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void branchReport(BranchReportRequestProto request, StreamObserver<BranchReportResponseProto> responseObserver) {
        final PbConvertor pbConvertor = ProtobufConvertManager.getInstance().fetchConvertor(request.getClass().getName());
        BranchReportRequest requestModel = (BranchReportRequest) pbConvertor.convert2Model(request);
        RpcMessageHandleContext handleContext = buildHandleContext(responseObserver);
        handleContext.setMessageReply(response -> {
            if (!(response instanceof BranchReportResponse)) {
                LOGGER.warn("[GRPC]wrong response type, need {} but actually {}", BranchReportResponse.class, response.getClass());
                return;
            }
            PbConvertor convertor = ProtobufConvertManager.getInstance().fetchConvertor(BranchReportResponse.class.getName());
            try {
                responseObserver.onNext((BranchReportResponseProto) convertor.convert2Proto(response));
                responseObserver.onCompleted();
            } catch (Exception e) {
                LOGGER.warn("[GRPC]fail to send response, req:{}, resp:{}", request, response);
            }
        });
        remotingServer.processMessage(handleContext, requestModel);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void lockQuery(GlobalLockQueryRequestProto request, StreamObserver<GlobalLockQueryResponseProto> responseObserver) {
        final PbConvertor pbConvertor = ProtobufConvertManager.getInstance().fetchConvertor(request.getClass().getName());
        GlobalLockQueryRequest requestModel = (GlobalLockQueryRequest) pbConvertor.convert2Model(request);
        RpcMessageHandleContext handleContext = buildHandleContext(responseObserver);
        handleContext.setMessageReply(response -> {
            if (!(response instanceof GlobalLockQueryResponse)) {
                LOGGER.warn("[GRPC]wrong response type, need {} but actually {}", GlobalLockQueryResponse.class, response.getClass());
                return;
            }
            PbConvertor convertor = ProtobufConvertManager.getInstance().fetchConvertor(GlobalLockQueryResponse.class.getName());
            try {
                responseObserver.onNext((GlobalLockQueryResponseProto) convertor.convert2Proto(response));
                responseObserver.onCompleted();
            } catch (Exception e) {
                LOGGER.warn("[GRPC]fail to send response, req:{}, resp:{}", request, response);
            }
        });
        remotingServer.processMessage(handleContext, requestModel);
    }

    public RpcMessageHandleContext buildHandleContext(StreamObserver<? extends Message> responseObserver) {
        SeataChannel curChannel = new GrpcSeataChannel(CUR_CONNECT_ID.get(), CUR_CONNECTION.get(), responseObserver);
        return new RpcMessageHandleContext(curChannel);
    }

    private boolean isProcessable(GrpcRemoting.BiStreamMessageType messageType) {
        return GrpcRemoting.BiStreamMessageType.TypeBranchCommitResult == messageType
                || GrpcRemoting.BiStreamMessageType.TypeBranchRollBackResult == messageType;
    }

    static class MessageTypeHelper {
        private static final Map<GrpcRemoting.BiStreamMessageType, Class> MESSAGE_TYPE_CLASS_MAP = new HashMap<>();

        static {
            MESSAGE_TYPE_CLASS_MAP.put(GrpcRemoting.BiStreamMessageType.TypeBranchCommit, io.seata.serializer.protobuf.generated.BranchCommitRequestProto.class);
            MESSAGE_TYPE_CLASS_MAP.put(GrpcRemoting.BiStreamMessageType.TypeBranchCommitResult, io.seata.serializer.protobuf.generated.BranchCommitResponseProto.class);
            MESSAGE_TYPE_CLASS_MAP.put(GrpcRemoting.BiStreamMessageType.TypeBranchRollback, io.seata.serializer.protobuf.generated.BranchRollbackRequestProto.class);
            MESSAGE_TYPE_CLASS_MAP.put(GrpcRemoting.BiStreamMessageType.TypeBranchRollBackResult, io.seata.serializer.protobuf.generated.BranchRollbackResponseProto.class);
            MESSAGE_TYPE_CLASS_MAP.put(GrpcRemoting.BiStreamMessageType.TYPERegisterRMRequest, RegisterRMRequestProto.class);
            MESSAGE_TYPE_CLASS_MAP.put(GrpcRemoting.BiStreamMessageType.TYPERegisterRMResponse, RegisterRMResponseProto.class);
            MESSAGE_TYPE_CLASS_MAP.put(GrpcRemoting.BiStreamMessageType.TypeRMUndoLogDelete, io.seata.serializer.protobuf.generated.UndoLogDeleteRequestProto.class);
        }

        public static Class getBiStreamMessageClassType(GrpcRemoting.BiStreamMessageType messageType) {
            return MESSAGE_TYPE_CLASS_MAP.get(messageType);
        }
    }
}
