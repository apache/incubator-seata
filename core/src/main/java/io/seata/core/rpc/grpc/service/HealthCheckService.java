package io.seata.core.rpc.grpc.service;

import com.google.protobuf.Message;
import io.grpc.stub.StreamObserver;
import io.seata.core.protocol.HeartbeatMessage;
import io.seata.core.rpc.SeataChannel;
import io.seata.core.rpc.grpc.GrpcRemotingServer;
import io.seata.core.rpc.grpc.GrpcSeataChannel;
import io.seata.core.rpc.grpc.generated.HealthCheckServiceGrpc;
import io.seata.core.rpc.processor.RpcMessageHandleContext;
import io.seata.serializer.protobuf.convertor.PbConvertor;
import io.seata.serializer.protobuf.generated.HeartbeatMessageProto;
import io.seata.serializer.protobuf.manager.ProtobufConvertManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.seata.core.rpc.grpc.ContextKeyConstants.CUR_CONNECTION;
import static io.seata.core.rpc.grpc.ContextKeyConstants.CUR_CONNECT_ID;

/**
 * @author goodboycoder
 */
public class HealthCheckService extends HealthCheckServiceGrpc.HealthCheckServiceImplBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(HealthCheckService.class);

    private final GrpcRemotingServer remotingServer;

    public HealthCheckService(GrpcRemotingServer remotingServer) {
        this.remotingServer = remotingServer;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void heartbeatChecker(HeartbeatMessageProto request, StreamObserver<HeartbeatMessageProto> responseObserver) {
        final PbConvertor pbConvertor = ProtobufConvertManager.getInstance().fetchConvertor(request.getClass().getName());
        HeartbeatMessage requestModel = (HeartbeatMessage) pbConvertor.convert2Model(request);
        RpcMessageHandleContext handleContext = buildHandleContext(responseObserver);
        handleContext.setMessageReply(response -> {
            if (!(response instanceof HeartbeatMessage)) {
                LOGGER.warn("[GRPC]wrong response type, need {} but actually {}", HeartbeatMessage.class, response.getClass());
                return;
            }
            PbConvertor convertor = ProtobufConvertManager.getInstance().fetchConvertor(HeartbeatMessage.class.getName());
            try {
                responseObserver.onNext((HeartbeatMessageProto) convertor.convert2Proto(response));
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
