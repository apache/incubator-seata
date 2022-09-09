package io.seata.server.storage.r2dbc.repository;

import io.seata.server.storage.r2dbc.entity.Lock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

/**
 * @author jianbin.chen
 */
@ConditionalOnExpression("#{'db'.equals('${sessionMode}')}")
public interface LockRepository extends ReactiveCrudRepository<Lock, String> {
}
