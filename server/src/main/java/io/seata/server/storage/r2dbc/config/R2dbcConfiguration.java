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
package io.seata.server.storage.r2dbc.config;

import java.time.Duration;
import javax.sql.DataSource;
import io.opentracing.contrib.jdbc.ConnectionInfo;
import io.opentracing.contrib.jdbc.parser.URLParser;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import io.seata.core.store.db.AbstractDataSourceProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
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
@ConditionalOnExpression("#{'db'.equals('${sessionMode}')||'db'.equals('${lockMode}')}")
@Configuration
public class R2dbcConfiguration extends AbstractDataSourceProvider {

    @Bean
    public ConnectionFactory connectionFactory() {
        ConnectionInfo connectionInfo = URLParser.parser(getUrl());
        String[] dbPeer = connectionInfo.getDbPeer().split(":");
        String host = dbPeer[0];
        int port = Integer.parseInt(dbPeer[1]);
        ConnectionFactoryOptions options = ConnectionFactoryOptions.builder().option(SSL, false)
            .option(DRIVER, getDBType().name().toLowerCase()).option(HOST, host).option(USER, getUser())
            .option(PORT, port).option(PASSWORD, getPassword()).option(DATABASE, connectionInfo.getDbInstance())
            .option(CONNECT_TIMEOUT, Duration.ofMillis(getMaxWait())).build();
        return ConnectionFactories.get(options);
    }

    @Bean
    public ReactiveTransactionManager reactiveTransactionManager(ConnectionFactory connectionFactory) {
        return new R2dbcTransactionManager(connectionFactory);
    }

    @Bean
    public R2dbcEntityTemplate r2dbcEntityTemplate(ConnectionFactory connectionFactory) {
        return new R2dbcEntityTemplate(connectionFactory);
    }

    @Override
    public DataSource generate() {
        return null;
    }

}
