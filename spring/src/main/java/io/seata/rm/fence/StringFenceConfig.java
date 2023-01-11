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
package io.seata.rm.fence;

import io.seata.common.exception.FrameworkErrorCode;
import io.seata.integrationapi.fence.config.CommonFenceConfig;
import io.seata.integrationapi.fence.exception.CommonFenceException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;

/**
 * @author leezongjie
 * @date 2022/12/17
 */
public class StringFenceConfig extends CommonFenceConfig implements InitializingBean {

    /**
     * Common fence datasource
     */
    private final DataSource dataSource;

    /**
     * Common fence transactionManager
     */
    private final PlatformTransactionManager transactionManager;

    public StringFenceConfig(DataSource dataSource, PlatformTransactionManager transactionManager) {
        this.dataSource = dataSource;
        this.transactionManager = transactionManager;
    }

    @Override
    public void afterPropertiesSet() {
        init();
        if (dataSource != null) {
            // set dataSource
            SpringFenceHandler.setDataSource(dataSource);
        } else {
            throw new CommonFenceException(FrameworkErrorCode.DateSourceNeedInjected);
        }
        if (transactionManager != null) {
            // set transaction template
            SpringFenceHandler.setTransactionTemplate(new TransactionTemplate(transactionManager));
        } else {
            throw new CommonFenceException(FrameworkErrorCode.TransactionManagerNeedInjected);
        }
    }


}
