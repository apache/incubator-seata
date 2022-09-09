package io.seata.server.storage.r2dbc.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.data.r2dbc.R2dbcDataAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@ConditionalOnExpression("#{'db'.equals('${sessionMode}')||'db'.equals('${lockMode}')}")
@Configuration
@Import(R2dbcDataAutoConfiguration.class)
public class R2dbcAutoConfiguration {
}
