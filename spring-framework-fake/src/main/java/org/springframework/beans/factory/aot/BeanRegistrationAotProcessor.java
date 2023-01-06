package org.springframework.beans.factory.aot;

import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.lang.Nullable;

@FunctionalInterface
public interface BeanRegistrationAotProcessor {

    @Nullable
    BeanRegistrationAotContribution processAheadOfTime(RegisteredBean registeredBean);

    default boolean isBeanExcludedFromAotProcessing() {
        return true;
    }

}
