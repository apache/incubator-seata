package io.seata.server.storage.r2dbc.repository;

import io.seata.server.storage.r2dbc.entity.GlobalTransaction;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import io.seata.server.storage.r2dbc.entity.Lock;

/**
 * @author jianbin.chen
 */
@ConditionalOnExpression("#{'db'.equals('${lockMode}')}")
public interface GlobalTransactionRepository extends ReactiveCrudRepository<GlobalTransaction, String> {
}
