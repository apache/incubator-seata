package com.alibaba.fescar.server.store.db;

import com.alibaba.fescar.common.exception.StoreException;
import com.alibaba.fescar.common.executor.Initialize;
import com.alibaba.fescar.common.loader.LoadLevel;
import com.alibaba.fescar.common.util.StringUtils;
import com.alibaba.fescar.config.Configuration;
import com.alibaba.fescar.config.ConfigurationFactory;
import com.alibaba.fescar.server.store.SessionStorable;
import com.alibaba.fescar.server.store.TransactionStoreManager;
import com.alibaba.fescar.server.store.TransactionWriteStore;
import org.apache.commons.dbcp.BasicDataSource;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author zhangsen
 * @data 2019/4/2
 */
@LoadLevel(name = "db")
public class DatabaseTransactionStoreManager implements TransactionStoreManager, Initialize {

    /**
     * The constant CONFIG.
     */
    protected static final Configuration CONFIG = ConfigurationFactory.getInstance();

    /**
     * is inited
     */
    protected AtomicBoolean inited = new AtomicBoolean(false);

    /**
     * the druid dataSource
     */
    protected BasicDataSource logStoreDataSource = null;

    public DatabaseTransactionStoreManager(){

    }

    @Override
    public synchronized void init(){
        if(inited.get()){
            return ;
        }
        String driver = CONFIG.getConfig("store.db.driver");
        String url = CONFIG.getConfig("store.db.url");
        String user = CONFIG.getConfig("store.db.user");
        String password = CONFIG.getConfig("store.db.password");
        if(StringUtils.isBlank(driver)){
            throw new StoreException("the {store.db.driver} is empty.");
        }
        if(StringUtils.isBlank(url)){
            throw new StoreException("the {store.db.url} is empty.");
        }
        if(StringUtils.isBlank(user)){
            throw new StoreException("the {store.db.user} is empty.");
        }
        if(StringUtils.isBlank(password)){
            throw new StoreException("the {store.db.password} is empty.");
        }
        //init dataSource
        logStoreDataSource = new BasicDataSource();
        logStoreDataSource.setDriverClassName(driver);
        logStoreDataSource.setUrl(url);
        logStoreDataSource.setUsername(user);
        logStoreDataSource.setPassword(password);
        logStoreDataSource.setInitialSize(1);
        logStoreDataSource.setMaxActive(CONFIG.getInt("store.db.maxConn", 100));
        logStoreDataSource.setMinIdle(1);
        logStoreDataSource.setMaxIdle(2);
        logStoreDataSource.setMaxWait(1000);
        logStoreDataSource.setTimeBetweenEvictionRunsMillis(120000);
        logStoreDataSource.setNumTestsPerEvictionRun(1);
        logStoreDataSource.setTestWhileIdle(true);
        logStoreDataSource.setValidationQuery("select 1");
        logStoreDataSource.setConnectionProperties("useUnicode=yes;characterEncoding=utf8;socketTimeout=5000;connectTimeout=500");

        inited.set(true);
    }

    @Override
    public boolean writeSession(LogOperation logOperation, SessionStorable session) {
        return false;
    }

    @Override
    public void shutdown() {
        try {
            logStoreDataSource.close();
        } catch (SQLException e) {
            throw new StoreException(e);
        }
    }

    @Override
    public List<TransactionWriteStore> readWriteStoreFromFile(int readSize, boolean isHistory) {
        return null;
    }

    @Override
    public boolean hasRemaining(boolean isHistory) {
        return false;
    }
}
