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
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import io.seata.core.store.db.AbstractDataSourceProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;


import static io.r2dbc.spi.ConnectionFactoryOptions.CONNECT_TIMEOUT;
import static io.r2dbc.spi.ConnectionFactoryOptions.DATABASE;
import static io.r2dbc.spi.ConnectionFactoryOptions.DRIVER;
import static io.r2dbc.spi.ConnectionFactoryOptions.HOST;
import static io.r2dbc.spi.ConnectionFactoryOptions.PASSWORD;
import static io.r2dbc.spi.ConnectionFactoryOptions.PORT;
import static io.r2dbc.spi.ConnectionFactoryOptions.USER;

@ConditionalOnExpression("#{'db'.equals('${sessionMode}')}")
@Configuration
public class R2dbcConfiguration extends AbstractDataSourceProvider {

    @Bean
    public ConnectionFactory connectionFactory() {
        ConnectionFactoryOptions options = ConnectionFactoryOptions.builder().option(DRIVER, getDBType().name().toLowerCase())
            .option(HOST, "127.0.0.1").option(USER, getUser()).option(PORT, 3306).option(PASSWORD, getPassword())
            .option(DATABASE, "seata").option(CONNECT_TIMEOUT, Duration.ofMillis(getMaxWait())).build();
        ConnectionFactory connectionFactory = ConnectionFactories.get(options);
        return connectionFactory;
    }

    @Bean
    public R2dbcEntityTemplate r2dbcEntityTemplate(ConnectionFactory connectionFactory) {
        R2dbcEntityTemplate template = new R2dbcEntityTemplate(connectionFactory);
        return template;
    }

    @Override
    public DataSource generate() {
        return null;
    }

}
