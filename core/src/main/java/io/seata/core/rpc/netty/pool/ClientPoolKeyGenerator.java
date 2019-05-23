package io.seata.core.rpc.netty.pool;

import io.seata.core.rpc.netty.NettyPoolKey;

/**
 * Client pool key generator.
 *
 * @author zhaojun
 */
public interface ClientPoolKeyGenerator {
    
    NettyPoolKey generate(String serverAddress);
}
