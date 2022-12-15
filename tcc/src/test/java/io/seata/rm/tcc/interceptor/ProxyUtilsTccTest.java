package io.seata.rm.tcc.interceptor;

import io.seata.commonapi.util.ProxyUtil;
import io.seata.core.context.RootContext;
import io.seata.core.exception.TransactionException;
import io.seata.core.model.BranchStatus;
import io.seata.core.model.BranchType;
import io.seata.core.model.Resource;
import io.seata.core.model.ResourceManager;
import io.seata.rm.DefaultResourceManager;
import io.seata.rm.tcc.NormalTccAction;
import io.seata.rm.tcc.NormalTccActionImpl;
import io.seata.rm.tcc.TccParam;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author leezongjie
 * @date 2022/12/14
 */
public class ProxyUtilsTccTest {

    private final String DEFAULT_XID = "default_xid";

    @Test
    public void testTcc() {
        //given
        NormalTccActionImpl tccAction = new NormalTccActionImpl();

        NormalTccAction tccActionProxy = ProxyUtil.createProxy(tccAction);

        RootContext.bind(DEFAULT_XID);

        TccParam tccParam = new TccParam(1, "abc@163.com");
        List<String> listB = Arrays.asList("b");

        AtomicReference<String> branchReference = new AtomicReference<String>();

        ResourceManager resourceManager = new ResourceManager() {

            @Override
            public Long branchRegister(BranchType branchType, String resourceId, String clientId, String xid, String applicationData, String lockKeys) throws TransactionException {
                branchReference.set(resourceId);
                return System.currentTimeMillis();
            }

            @Override
            public void branchReport(BranchType branchType, String xid, long branchId, BranchStatus status, String applicationData) throws TransactionException {

            }

            @Override
            public boolean lockQuery(BranchType branchType, String resourceId, String xid, String lockKeys) throws TransactionException {
                return false;
            }

            @Override
            public BranchStatus branchCommit(BranchType branchType, String xid, long branchId, String resourceId, String applicationData) throws TransactionException {
                return null;
            }

            @Override
            public BranchStatus branchRollback(BranchType branchType, String xid, long branchId, String resourceId, String applicationData) throws TransactionException {
                return null;
            }

            @Override
            public void registerResource(Resource resource) {

            }

            @Override
            public void unregisterResource(Resource resource) {

            }

            @Override
            public Map<String, Resource> getManagedResources() {
                return null;
            }

            @Override
            public BranchType getBranchType() {
                return null;
            }
        };

        DefaultResourceManager.mockResourceManager(BranchType.TCC, resourceManager);

        //when
        String result = tccActionProxy.prepare(null, 0, listB, tccParam);

        //then
        Assertions.assertEquals("a", result);
        Assertions.assertNotNull(result);
        Assertions.assertEquals("tccActionForTest", branchReference.get());

    }


}
