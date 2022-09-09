package io.seata.server.storage.r2dbc.repository;

import io.seata.server.storage.r2dbc.entity.DistributedLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
/**
 * @author jianbin.chen
 */
@ConditionalOnExpression("#{'db'.equals('${sessionMode}')}")
public interface DistributedLockRepository extends ReactiveCrudRepository<DistributedLock, String> {
}
