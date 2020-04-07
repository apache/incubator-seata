package io.seata.server.storage.mongo;

import java.util.ArrayList;
import java.util.List;

import io.seata.core.constants.ConfigurationKeys;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;

import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;

/**
 * @author funkye
 */
public class MongoPooledFactory {

    /**
     * The constant LOGGER.
     */
    protected static final Logger LOGGER = LoggerFactory.getLogger(MongoPooledFactory.class);

    private static volatile MongoClient mongoClient = null;

    private static final String DEFAULT_BRANCH_TABLE_NAME="branch_table";

    private static final String DEFAULT_GLOBAL_TABLE_NAME="global_table";

    private static final String DEFAULT_LOCK_TABLE_NAME="lock_table";

    private static String DEFAULT_DB_NAME="seata";

    private static final String HOST = "127.0.0.1";

    private static final int PORT = 27017;

    private static final String USERNAME = "mongo";

    private static final String PASSWORD = "mongo";

    private static final Configuration CONFIGURATION = ConfigurationFactory.getInstance();

    /**
     * get the mongo db pool instance (singleton)
     *
     * @return mongoClient
     */
    private static MongoClient getMongoPoolInstance() {
        if (mongoClient == null) {
            synchronized (MongoPooledFactory.class) {
                if (mongoClient == null) {
                    MongoClientOptions.Builder build = new MongoClientOptions.Builder();
                    build.connectionsPerHost(50);
                    build.maxWaitTime(1000 * 60 * 2);
                    build.connectTimeout(1000 * 60 * 1);
                    MongoClientOptions mongoClientOptions = build.build();
                    build.threadsAllowedToBlockForConnectionMultiplier(50);
                    ServerAddress serverAddress = new ServerAddress(CONFIGURATION.getConfig(
                        ConfigurationKeys.STORE_MONGO_HOST,HOST), CONFIGURATION.getInt(
                        ConfigurationKeys.STORE_MONGO_PORT,PORT));
                    List<ServerAddress> address = new ArrayList<ServerAddress>();
                    address.add(serverAddress);
                    DEFAULT_DB_NAME=CONFIGURATION.getConfig(
                        ConfigurationKeys.STORE_MONGO_DATA_BASE_NAME,DEFAULT_DB_NAME);
                    MongoCredential credential =
                        MongoCredential.createScramSha1Credential(CONFIGURATION.getConfig(
                            ConfigurationKeys.STORE_MONGO_USERNAME,USERNAME), DEFAULT_DB_NAME, CONFIGURATION.getConfig(
                            ConfigurationKeys.STORE_MONGO_PASSWORD,PASSWORD).toCharArray());
                    List<MongoCredential> credentials = new ArrayList<MongoCredential>();
                    credentials.add(credential);
                    mongoClient = new MongoClient(address, credentials, mongoClientOptions);
                    if(LOGGER.isInfoEnabled()){
                        LOGGER.info("initialization of the build mongo db connection pool is complete");
                    }
                }
            }
        }
        return mongoClient;
    }

    public static MongoCollection<Document> getLockCollection() {
        return getMongoPoolInstance().getDatabase(DEFAULT_DB_NAME).getCollection(DEFAULT_LOCK_TABLE_NAME);
    }

    public static MongoCollection<Document> getGlobalCollection() {
        return getMongoPoolInstance().getDatabase(DEFAULT_DB_NAME).getCollection(DEFAULT_GLOBAL_TABLE_NAME);
    }

    public static MongoCollection<Document> getBranchCollection() {
        return getMongoPoolInstance().getDatabase(DEFAULT_DB_NAME).getCollection(DEFAULT_BRANCH_TABLE_NAME);
    }

}
