package io.seata.core.rpc.grpc.norelativeimpl;

import io.seata.common.loader.EnhancedServiceLoader;

public abstract class TmRpcCallableClient implements RpcCallableClient {
    public static final TmRpcCallableClient getInstance(){
        return SingletonHolder.getInstance();
    }

    static private class SingletonHolder{
        private static  volatile TmRpcCallableClient singleton;
        private static TmRpcCallableClient getInstance(){
            if (singleton==null){
                synchronized (SingletonHolder.class) {
                    if (singleton == null) {
                        singleton = EnhancedServiceLoader.load(TmRpcCallableClient.class);
                    }
                }
            }
            return singleton;
        }
    }
}
