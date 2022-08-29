package io.seata.core.rpc;

/**
 * @author goodboycoder
 */
public interface RmRemotingClient extends RemotingClient{

    /**
     * Register new db key.
     *
     * @param resourceGroupId the resource group id
     * @param resourceId      the db key
     */
    void registerResource(String resourceGroupId, String resourceId);
}
