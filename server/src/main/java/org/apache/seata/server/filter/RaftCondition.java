package org.apache.seata.server.filter;

import org.apache.seata.common.store.SessionMode;
import org.apache.seata.server.store.StoreConfig;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class RaftCondition implements Condition {
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return StoreConfig.getSessionMode() == SessionMode.RAFT;
    }
}
