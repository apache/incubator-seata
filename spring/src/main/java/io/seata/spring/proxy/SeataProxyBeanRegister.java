package io.seata.spring.proxy;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import io.seata.common.util.CollectionUtils;
import io.seata.common.util.ReflectionUtil;
import org.springframework.context.annotation.Lazy;

/**
 * Seata Proxy Bean Register
 *
 * @author wang.liang
 * @see SeataProxyScanner
 */
@Lazy(false)
public class SeataProxyBeanRegister {

    private Set<Class<?>> beanClasses = new HashSet<>();
    private Set<String> beanNames = new HashSet<>();

    public void addBeanClasses(Class<?>... beanClasses) {
        CollectionUtils.addAll(this.beanClasses, beanClasses);
    }

    public void addBeanClassNames(String... beanClassNames) {
        if (CollectionUtils.isNotEmpty(beanClassNames)) {
            CollectionUtils.addAll(this.beanClasses, ReflectionUtil.classNamesToClassSet(Arrays.asList(beanClassNames)));
        }
    }

    public void addBeanNames(String... beanNames) {
        CollectionUtils.addAll(this.beanNames, beanNames);
    }

    public Set<Class<?>> getBeanClasses() {
        return beanClasses;
    }

    public Set<String> getBeanNames() {
        return beanNames;
    }
}
