package io.seata.core.rpc.grpc.service;

import com.google.protobuf.Message;
import io.grpc.stub.StreamObserver;
import io.seata.core.protocol.RegisterTMRequest;
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
import io.seata.core.rpc.grpc.GrpcRemotingServer;
import io.seata.core.rpc.grpc.GrpcSeataChannel;
import io.seata.core.rpc.grpc.generated.TransactionManagerServiceGrpc;
import io.seata.core.rpc.processor.RpcMessageHandleContext;
import io.seata.serializer.protobuf.convertor.PbConvertor;
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
import io.seata.serializer.protobuf.generated.RegisterTMRequestProto;
import io.seata.serializer.protobuf.generated.RegisterTMResponseProto;
import io.seata.serializer.protobuf.manager.ProtobufConvertManager;
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

    @SuppressWarnings("unchecked")
    @Override
    public void registerTM(RegisterTMRequestProto request, StreamObserver<RegisterTMResponseProto> responseObserver) {
        final PbConvertor pbConvertor = ProtobufConvertManager.getInstance().fetchConvertor(request.getClass().getName());
        RegisterTMRequest requestModel = (RegisterTMRequest) pbConvertor.convert2Model(request);

        RpcMessageHandleContext handleContext = buildHandleContext(responseObserver);
        handleContext.setMessageReply(response -> {
            if (!(response instanceof RegisterTMResponse)) {
                LOGGER.warn("[GRPC]wrong response type, need {} but actually {}", RegisterTMResponse.class, response.getClass());
                return;
            }
            PbConvertor convertor = ProtobufConvertManager.getInstance().fetchConvertor(RegisterTMResponse.class.getName());
            try {
                responseObserver.onNext((RegisterTMResponseProto) convertor.convert2Proto(response));
                responseObserver.onCompleted();
            } catch (Exception e) {
                LOGGER.warn("[GRPC]fail to send response, req:{}, resp:{}", request, response);
            }
        });
        remotingServer.processMessage(handleContext, requestModel);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void globalBegin(GlobalBeginRequestProto request, StreamObserver<GlobalBeginResponseProto> responseObserver) {
        final PbConvertor pbConvertor = ProtobufConvertManager.getInstance().fetchConvertor(request.getClass().getName());
        GlobalBeginRequest requestModel = (GlobalBeginRequest) pbConvertor.convert2Model(request);
        RpcMessageHandleContext handleContext = buildHandleContext(responseObserver);
        handleContext.setMessageReply(response -> {
            if (!(response instanceof GlobalBeginResponse)) {
                LOGGER.warn("[GRPC]wrong response type, need {} but actually {}", GlobalBeginResponse.class, response.getClass());
                return;
            }
            PbConvertor convertor = ProtobufConvertManager.getInstance().fetchConvertor(GlobalBeginResponse.class.getName());
            try {
                responseObserver.onNext((GlobalBeginResponseProto) convertor.convert2Proto(response));
                responseObserver.onCompleted();
            } catch (Exception e) {
                LOGGER.warn("[GRPC]fail to send response, req:{}, resp:{}", request, response);
            }
        });
        remotingServer.processMessage(handleContext, requestModel);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void globalCommit(GlobalCommitRequestProto request, StreamObserver<GlobalCommitResponseProto> responseObserver) {
        final PbConvertor pbConvertor = ProtobufConvertManager.getInstance().fetchConvertor(request.getClass().getName());
        GlobalCommitRequest requestModel = (GlobalCommitRequest) pbConvertor.convert2Model(request);
        RpcMessageHandleContext handleContext = buildHandleContext(responseObserver);
        handleContext.setMessageReply(response -> {
            if (!(response instanceof GlobalCommitResponse)) {
                LOGGER.warn("[GRPC]wrong response type, need {} but actually {}", GlobalCommitResponse.class, response.getClass());
                return;
            }
            PbConvertor convertor = ProtobufConvertManager.getInstance().fetchConvertor(GlobalCommitResponse.class.getName());
            try {
                responseObserver.onNext((GlobalCommitResponseProto) convertor.convert2Proto(response));
                responseObserver.onCompleted();
            } catch (Exception e) {
                LOGGER.warn("[GRPC]fail to send response, req:{}, resp:{}", request, response);
            }
        });
        remotingServer.processMessage(handleContext, requestModel);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void globalRollback(GlobalRollbackRequestProto request, StreamObserver<GlobalRollbackResponseProto> responseObserver) {
        final PbConvertor pbConvertor = ProtobufConvertManager.getInstance().fetchConvertor(request.getClass().getName());
        GlobalRollbackRequest requestModel = (GlobalRollbackRequest) pbConvertor.convert2Model(request);
        RpcMessageHandleContext handleContext = buildHandleContext(responseObserver);
        handleContext.setMessageReply(response -> {
            if (!(response instanceof GlobalRollbackResponse)) {
                LOGGER.warn("[GRPC]wrong response type, need {} but actually {}", GlobalRollbackResponse.class, response.getClass());
                return;
            }
            PbConvertor convertor = ProtobufConvertManager.getInstance().fetchConvertor(GlobalRollbackResponse.class.getName());
            try {
                responseObserver.onNext((GlobalRollbackResponseProto) convertor.convert2Proto(response));
                responseObserver.onCompleted();
            } catch (Exception e) {
                LOGGER.warn("[GRPC]fail to send response, req:{}, resp:{}", request, response);
            }
        });
        remotingServer.processMessage(handleContext, requestModel);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void globalReport(GlobalReportRequestProto request, StreamObserver<GlobalReportResponseProto> responseObserver) {
        final PbConvertor pbConvertor = ProtobufConvertManager.getInstance().fetchConvertor(request.getClass().getName());
        GlobalReportRequest requestModel = (GlobalReportRequest) pbConvertor.convert2Model(request);
        RpcMessageHandleContext handleContext = buildHandleContext(responseObserver);
        handleContext.setMessageReply(response -> {
            if (!(response instanceof GlobalReportResponse)) {
                LOGGER.warn("[GRPC]wrong response type, need {} but actually {}", GlobalReportResponse.class, response.getClass());
                return;
            }
            PbConvertor convertor = ProtobufConvertManager.getInstance().fetchConvertor(GlobalReportResponse.class.getName());
            try {
                responseObserver.onNext((GlobalReportResponseProto) convertor.convert2Proto(response));
                responseObserver.onCompleted();
            } catch (Exception e) {
                LOGGER.warn("[GRPC]fail to send response, req:{}, resp:{}", request, response);
            }
        });
        remotingServer.processMessage(handleContext, requestModel);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void getGlobalStatus(GlobalStatusRequestProto request, StreamObserver<GlobalStatusResponseProto> responseObserver) {
        final PbConvertor pbConvertor = ProtobufConvertManager.getInstance().fetchConvertor(request.getClass().getName());
        GlobalStatusRequest requestModel = (GlobalStatusRequest) pbConvertor.convert2Model(request);
        RpcMessageHandleContext handleContext = buildHandleContext(responseObserver);
        handleContext.setMessageReply(response -> {
            if (!(response instanceof GlobalStatusResponse)) {
                LOGGER.warn("[GRPC]wrong response type, need {} but actually {}", GlobalStatusResponse.class, response.getClass());
                return;
            }
            PbConvertor convertor = ProtobufConvertManager.getInstance().fetchConvertor(GlobalStatusResponse.class.getName());
            try {
                responseObserver.onNext((GlobalStatusResponseProto) convertor.convert2Proto(response));
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
}
