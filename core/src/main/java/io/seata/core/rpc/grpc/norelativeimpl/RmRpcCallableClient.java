package io.seata.core.rpc.grpc.norelativeimpl;

import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.core.model.ResourceManager;
import io.seata.core.rpc.TransactionMessageHandler;

public abstract class RmRpcCallableClient implements RpcCallableClient {
    public static final RmRpcCallableClient getInstance(){
        return SingletonHolder.getInstance();
    }

    private ResourceManager resourceManager;
    private TransactionMessageHandler handler;

    static private class SingletonHolder{
        private static  volatile RmRpcCallableClient singleton;
        private static RmRpcCallableClient getInstance(){
            if (singleton==null){
                synchronized (SingletonHolder.class) {
                    if (singleton == null) {
                        singleton = EnhancedServiceLoader.load(RmRpcCallableClient.class);
                    }
                }
            }
            return singleton;
        }
    }

    public abstract void initialize(String applicationId, String transactionServiceGroup,ResourceManager rm,TransactionMessageHandler handler);

    @Override
    public void initialize(String applicationId, String transactionServiceGroup) {
        if (this.resourceManager == null || this.handler == null) {
            throw new RuntimeException("have not resource manager and handler");
        }
        initialize(applicationId,transactionServiceGroup,this.resourceManager,this.handler);
    }
    public void setResourceManager(ResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }

    public void setHandler(TransactionMessageHandler handler) {
        this.handler = handler;
    }

    public abstract void registerResource(String resourceGroupId, String resourceId);
}
