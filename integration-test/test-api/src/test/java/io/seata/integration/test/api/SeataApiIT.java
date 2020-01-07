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
package io.seata.integration.test.api;

import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import io.seata.core.exception.TransactionException;
import io.seata.integration.test.api.service.impl.AccountServiceImpl;
import io.seata.integration.test.api.service.impl.OrderServiceImpl;
import io.seata.integration.test.api.service.impl.StorageServiceImpl;
import io.seata.rm.RMClient;
import io.seata.integration.test.api.service.AccountService;
import io.seata.integration.test.api.service.OrderService;
import io.seata.integration.test.api.service.StorageService;
import io.seata.tm.TMClient;
import io.seata.tm.api.GlobalTransaction;
import io.seata.tm.api.GlobalTransactionContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * The type Bussiness.
 * <p>
 * Using API to build distributed transaction non spring environment of single application multi data source
 */
public class SeataApiIT {

    /**
     * The entry point of application.
     *
     * @throws SQLException         the sql exception
     * @throws TransactionException the transaction exception
     */
    @Test
    public void testSeataApi() throws SQLException, TransactionException, InterruptedException {

        String userId = "U100001";
        String commodityCode = "C00321";
        int commodityCount = 100;
        int money = 999;
        AccountService accountService = new AccountServiceImpl();
        StorageService storageService = new StorageServiceImpl();
        OrderService orderService = new OrderServiceImpl();
        orderService.setAccountService(accountService);

        //reset data
        accountService.reset(userId, String.valueOf(money));
        storageService.reset(commodityCode, String.valueOf(commodityCount));
        orderService.reset(null, null);

        //init seata; only once
        String applicationId = "api";
        String txServiceGroup = "my_test_tx_group";
        TMClient.init(applicationId, txServiceGroup);
        RMClient.init(applicationId, txServiceGroup);

        //trx
        GlobalTransaction tx = GlobalTransactionContext.getCurrentOrCreate();
        try {
            tx.begin(60000, "testBiz");
            System.out.println("begin trx, xid is " + tx.getXid());

            //biz operate 3 dataSources
            //set >=5 will be rollback(200*5>999) else will be commit
            int opCount = 5;
            storageService.deduct(commodityCode, opCount);
            orderService.create(userId, commodityCode, opCount);

            //check data if negative
            boolean needCommit = ((StorageServiceImpl) storageService).validNegativeCheck("count", commodityCode)
                    && ((AccountServiceImpl) accountService).validNegativeCheck("money", userId);

            //if data negative rollback else commit
            if (needCommit) {
                tx.commit();
            } else {
                System.out.println("rollback trx, cause: data negative, xid is " + tx.getXid());
                tx.rollback();
            }
        } catch (Exception exx) {
            System.out.println("rollback trx, cause: " + exx.getMessage() + " , xid is " + tx.getXid());
            tx.rollback();
            throw exx;
        }
        TimeUnit.SECONDS.sleep(10);
        Assertions.assertEquals("","");
    }
}
