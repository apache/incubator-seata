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
package io.seata.integration.brpc;

import com.baidu.brpc.client.BrpcProxy;
import com.baidu.brpc.client.RpcClient;
import com.baidu.brpc.client.RpcClientOptions;
import com.baidu.brpc.server.RpcServer;
import com.baidu.brpc.server.RpcServerOptions;
import io.seata.core.context.RootContext;
import io.seata.core.model.BranchType;
import io.seata.integration.brpc.dto.Echo;
import io.seata.integration.brpc.server.EchoService;
import io.seata.integration.brpc.server.impl.EchoServiceImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author mxz0828@163.com
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
        Echo.EchoRequest.Builder echoRequest = Echo.EchoRequest.newBuilder();
        echoRequest.setReqMsg("WITH-TEST");
        Echo.EchoResponse echoResponse = echoAPI.echo(echoRequest.build());
        assertThat(echoResponse.getXid()).isEqualTo(DEFAULT_XID);
        RootContext.unbind();
        RootContext.unbindBranchType();
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
