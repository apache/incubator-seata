package io.seata.integration.brpc.server;

import com.baidu.brpc.protocol.BrpcMeta;
import io.seata.integration.brpc.dto.EchoRequest;
import io.seata.integration.brpc.dto.EchoResponse;

/**
 * @author mxz0828@163.com
 * @date 2021/8/16
 */
public interface EchoService {


    @BrpcMeta(serviceName = "echoAPI", methodName = "echo")
    EchoResponse echo(EchoRequest request);


}
