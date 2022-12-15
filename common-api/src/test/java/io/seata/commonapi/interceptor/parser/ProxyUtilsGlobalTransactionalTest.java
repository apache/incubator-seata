package io.seata.commonapi.interceptor.parser;

import io.seata.commonapi.util.ProxyUtil;
import io.seata.core.exception.TransactionException;
import io.seata.core.model.GlobalStatus;
import io.seata.core.model.TransactionManager;
import io.seata.tm.TransactionManagerHolder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

/**
 * @author leezongjie
 * @date 2022/12/14
 */
public class ProxyUtilsGlobalTransactionalTest {

    private final String DEFAULT_XID = "default_xid";


    @Test
    public void testTcc() {
        //given
        BusinessImpl business = new BusinessImpl();

        AtomicReference<String> branchReference = new AtomicReference<String>();

        Business businessProxy = ProxyUtil.createProxy(business);

        TransactionManager mockTransactionManager = new TransactionManager() {
            @Override
            public String begin(String applicationId, String transactionServiceGroup, String name, int timeout) throws TransactionException {
                return DEFAULT_XID;
            }

            @Override
            public GlobalStatus commit(String xid) throws TransactionException {
                return GlobalStatus.Committed;
            }

            @Override
            public GlobalStatus rollback(String xid) throws TransactionException {
                return null;
            }

            @Override
            public GlobalStatus getStatus(String xid) throws TransactionException {
                return null;
            }

            @Override
            public GlobalStatus globalReport(String xid, GlobalStatus globalStatus) throws TransactionException {
                return null;
            }
        };

        TransactionManagerHolder.set(mockTransactionManager);

        //when
        String result = businessProxy.doBiz("test");

        //then
        Assertions.assertNotNull(result);

    }


}
