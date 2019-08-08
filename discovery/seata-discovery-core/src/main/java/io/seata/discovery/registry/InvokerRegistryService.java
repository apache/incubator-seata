package io.seata.discovery.registry;

import io.seata.discovery.loadbalance.ServerRegistration;

import java.util.List;

public interface InvokerRegistryService extends RegistryService{

    /**
     * register the serverRegistration
     *
     * @param serverRegistration
     * @throws Exception
     */
    void registerInvoker(ServerRegistration serverRegistration) throws Exception;

    /**
     * unregister the serverRegistration
     *
     * @param serverRegistration
     * @throws Exception
     */
    void unRegisterInvoker(ServerRegistration serverRegistration) throws Exception;
    /**
     * Lookup list.
     *
     * @param key the key
     * @return the list
     * @throws Exception the exception
     */
    List<ServerRegistration> lookup(String key) throws Exception;

    /**
     * Close.
     * @throws Exception
     */
    void close() throws Exception;
}
