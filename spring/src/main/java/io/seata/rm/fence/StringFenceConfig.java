package io.seata.rm.fence;

import io.seata.common.exception.FrameworkErrorCode;
import io.seata.commonapi.fence.config.CommonFenceConfig;
import io.seata.commonapi.fence.exception.CommonFenceException;
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
