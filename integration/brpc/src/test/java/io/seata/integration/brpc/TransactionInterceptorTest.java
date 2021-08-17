package io.seata.integration.brpc;

import com.baidu.brpc.client.BrpcProxy;
import com.baidu.brpc.client.RpcClient;
import com.baidu.brpc.client.RpcClientOptions;
import com.baidu.brpc.interceptor.ServerInvokeInterceptor;
import com.baidu.brpc.server.RpcServer;
import com.baidu.brpc.server.RpcServerOptions;
import com.google.common.collect.Lists;
import io.seata.core.context.RootContext;
import io.seata.core.model.BranchType;
import io.seata.integration.brpc.dto.EchoRequest;
import io.seata.integration.brpc.dto.EchoResponse;
import io.seata.integration.brpc.server.EchoService;
import io.seata.integration.brpc.server.impl.EchoServiceImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author mxz0828@163.com
 * @date 2021/8/16
 */
public class TransactionInterceptorTest {

    /**
     * client and server with TM AND RM role
     */
    private static RpcServer rpcServerB;

    private static final String DEFAULT_XID = "XID_FOR_BRPC_TEST";

    @Test
    public void testWithInterceptor() {

        // within transaction interceptor should propagate XID and branchType
        RootContext.bind(DEFAULT_XID);
        RootContext.bindBranchType(BranchType.AT);
        RpcClient rpcClientA = initRpcClient();
        EchoService echoAPI = BrpcProxy.getProxy(rpcClientA, EchoService.class);
        EchoRequest echoRequest = new EchoRequest();
        echoRequest.setReqMsg("WITH-TEST");
        EchoResponse echoResponse = echoAPI.echo(echoRequest);
        assertThat(echoResponse.getXid()).isEqualTo(DEFAULT_XID);
        RootContext.unbind();
        RootContext.unbindBranchType();
        rpcServerB.shutdown();
    }

    @Test
    public void testWithoutInterceptor() {

        // without transaction interceptor should no exception and no xid
        RpcClient rpcClientA = initRpcClient();
        EchoRequest echoRequest = new EchoRequest();
        echoRequest.setReqMsg("WITHOUT-TEST");
        rpcServerB.getInterceptors().clear();
        rpcServerB.getInterceptors().addAll(Lists.newArrayList(new ServerInvokeInterceptor()));
        EchoService echoAPI = BrpcProxy.getProxy(rpcClientA, EchoService.class);
        EchoResponse echoResponse = echoAPI.echo(echoRequest);
        assertThat(echoResponse.getXid()).isNull();
        rpcClientA.stop();
        rpcServerB.shutdown();
    }

    private RpcClient initRpcClient() {
        // ----------------------------- rpc client init -----------------------------
        RpcClientOptions rpcClientAOptions = new RpcClientOptions();
        rpcClientAOptions.setIoThreadNum(1);
        rpcClientAOptions.setWorkThreadNum(1);
        rpcClientAOptions.setMinIdleConnections(1);
        rpcClientAOptions.setReadTimeoutMillis(999999);
        RpcClient rpcClient = new RpcClient("list://127.0.0.1:9999", rpcClientAOptions);
        rpcClient.getInterceptors().add(new TransactionPropagationClientInterceptor());
        return rpcClient;
    }


    @BeforeAll
    public static void rpcInit() {

        // ----------------------------- rpc server init -----------------------------
        RpcServerOptions rpcServerBOptions = new RpcServerOptions();
        rpcServerBOptions.setIoThreadNum(1);
        rpcServerBOptions.setWorkThreadNum(1);
        rpcServerB = new RpcServer(9999, rpcServerBOptions);
        rpcServerB.registerService(new EchoServiceImpl());
        rpcServerB.getInterceptors().add(new TransactionPropagationServerInterceptor());
        rpcServerB.start();
    }


}
