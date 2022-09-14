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
package io.seata.server.storage.db.r2dbc.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.sql.DataSource;
import io.opentracing.contrib.jdbc.ConnectionInfo;
import io.opentracing.contrib.jdbc.parser.URLParser;
import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import io.seata.common.util.StringUtils;
import io.seata.core.store.db.AbstractDataSourceProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.convert.CustomConversions;
import org.springframework.data.r2dbc.convert.MappingR2dbcConverter;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.dialect.DialectResolver;
import org.springframework.data.r2dbc.dialect.R2dbcDialect;
import org.springframework.data.r2dbc.mapping.R2dbcMappingContext;
import org.springframework.data.relational.core.mapping.NamingStrategy;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.transaction.ReactiveTransactionManager;

import static io.r2dbc.spi.ConnectionFactoryOptions.CONNECT_TIMEOUT;
import static io.r2dbc.spi.ConnectionFactoryOptions.DATABASE;
import static io.r2dbc.spi.ConnectionFactoryOptions.DRIVER;
import static io.r2dbc.spi.ConnectionFactoryOptions.HOST;
import static io.r2dbc.spi.ConnectionFactoryOptions.PASSWORD;
import static io.r2dbc.spi.ConnectionFactoryOptions.PORT;
import static io.r2dbc.spi.ConnectionFactoryOptions.SSL;
import static io.r2dbc.spi.ConnectionFactoryOptions.USER;

/**
 * @author funkye
 */
@ConditionalOnExpression("#{'r2dbc'.equals('${store.db.type}')}")
@Configuration
@EnableConfigurationProperties(R2dbcProperties.class)
@AutoConfigureBefore(R2dbcAutoConfiguration.class)
public class R2dbcConfiguration extends AbstractDataSourceProvider {

    @Bean
    public DatabaseClient databaseClient(ConnectionFactory connectionFactory) {
        R2dbcDialect dialect = DialectResolver.getDialect(connectionFactory);
        return DatabaseClient.builder().connectionFactory(connectionFactory)
            .bindMarkers(dialect.getBindMarkersFactory()).build();
    }

    @Bean
    public ReactiveTransactionManager reactiveTransactionManager(ConnectionFactory connectionFactory) {
        return new R2dbcTransactionManager(connectionFactory);
    }

    @Bean
    public R2dbcEntityTemplate r2dbcEntityTemplate(DatabaseClient databaseClient) {
        R2dbcDialect dialect = DialectResolver.getDialect(databaseClient.getConnectionFactory());
        return new R2dbcEntityTemplate(databaseClient, dialect);
    }

    @Bean(destroyMethod = "dispose")
    ConnectionPool connectionFactory(R2dbcProperties r2dbcProperties) {
        String url = getUrl();
        ConnectionInfo connectionInfo = URLParser.parser(url);
        String[] dbPeer = connectionInfo.getDbPeer().split(":");
        String host = dbPeer[0];
        int port = Integer.parseInt(dbPeer[1]);
        ConnectionFactoryOptions.Builder options = ConnectionFactoryOptions.builder()
            .option(DRIVER, getDBType().name().toLowerCase()).option(HOST, host).option(USER, getUser())
            .option(PORT, port).option(PASSWORD, getPassword()).option(DATABASE, connectionInfo.getDbInstance())
            .option(CONNECT_TIMEOUT, Duration.ofMillis(getMaxWait()));
        String paramUrl = url.substring(url.indexOf("?") + 1);
        if (StringUtils.isNotBlank(paramUrl)) {
            String useSSL = "useSSL";
            if (paramUrl.contains(useSSL)) {
                String[] params = paramUrl.split("&");
                for (String param : params) {
                    if (param.contains(useSSL)) {
                        options.option(SSL, Boolean.parseBoolean(param.split("=")[1]));
                        break;
                    }
                }
            }
        }
        ConnectionFactory connectionFactory = ConnectionFactories.get(options.build());
        R2dbcProperties.Pool pool = r2dbcProperties.getPool();
        PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
        ConnectionPoolConfiguration.Builder builder = ConnectionPoolConfiguration.builder(connectionFactory);
        map.from(Duration.ofMillis(getMaxWait())).to(builder::maxIdleTime);
        map.from(pool.getMaxAcquireTime()).to(builder::maxAcquireTime);
        map.from(pool.getMaxCreateConnectionTime()).to(builder::maxCreateConnectionTime);
        map.from(getMinConn()).to(builder::initialSize);
        map.from(getMaxConn()).to(builder::maxSize);
        map.from(pool.getValidationQuery()).whenHasText().to(builder::validationQuery);
        map.from(pool.getValidationDepth()).to(builder::validationDepth);
        return new ConnectionPool(builder.build());
    }

    @Bean
    public R2dbcMappingContext r2dbcMappingContext(ObjectProvider<NamingStrategy> namingStrategy,
        R2dbcCustomConversions r2dbcCustomConversions) {
        R2dbcMappingContext relationalMappingContext =
            new R2dbcMappingContext(namingStrategy.getIfAvailable(() -> NamingStrategy.INSTANCE));
        relationalMappingContext.setSimpleTypeHolder(r2dbcCustomConversions.getSimpleTypeHolder());
        return relationalMappingContext;
    }

    @Bean
    public MappingR2dbcConverter r2dbcConverter(R2dbcMappingContext mappingContext,
        R2dbcCustomConversions r2dbcCustomConversions) {
        return new MappingR2dbcConverter(mappingContext, r2dbcCustomConversions);
    }

    @Bean
    public R2dbcCustomConversions r2dbcCustomConversions(ConnectionFactory connectionFactory) {
        R2dbcDialect dialect = DialectResolver.getDialect(connectionFactory);
        List<Object> converters = new ArrayList<>(dialect.getConverters());
        converters.addAll(R2dbcCustomConversions.STORE_CONVERTERS);
        return new R2dbcCustomConversions(
            CustomConversions.StoreConversions.of(dialect.getSimpleTypeHolder(), converters), Collections.emptyList());
    }

    @Override
    public DataSource generate() {
        return null;
    }

}
