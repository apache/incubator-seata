package io.seata.core.rpc.netty.pool;

import io.seata.core.protocol.RegisterTMRequest;
import io.seata.core.rpc.netty.NettyPoolKey;

/**
 * TM client pool key generator.
 *
 * @author zhaojun
 */
public class TmClientPoolKeyGenerator implements ClientPoolKeyGenerator {
    
    private final String serverAddress;
    
    private final String applicationId;
    
    private final String transactionServiceGroup;
    
    public TmClientPoolKeyGenerator(final String serverAddress, final String applicationId, final String transactionServiceGroup) {
        this.serverAddress = serverAddress;
        this.applicationId = applicationId;
        this.transactionServiceGroup = transactionServiceGroup;
    }
    
    @Override
    public NettyPoolKey generate() {
        RegisterTMRequest message = new RegisterTMRequest(applicationId, transactionServiceGroup);
        return new NettyPoolKey(NettyPoolKey.TransactionRole.TMROLE, serverAddress, message);
    }
}
