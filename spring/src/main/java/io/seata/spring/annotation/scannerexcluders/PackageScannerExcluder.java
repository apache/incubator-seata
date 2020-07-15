package io.seata.spring.annotation.scannerexcluders;

import io.seata.common.loader.LoadLevel;
import io.seata.spring.annotation.ScannerExcluder;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.config.BeanDefinition;

import java.util.HashSet;
import java.util.Set;

/**
 * Package scanner excluder.
 *
 * @author wang.liang
 */
@LoadLevel(name = "Packages", order = 100)
public class PackageScannerExcluder implements ScannerExcluder {

    /**
     * The packages need to scan
     */
    private static final Set<String> SCANNABLE_PACKAGE_SET = new HashSet<>();

    /**
     * Add more packages.
     *
     * @param packages the packages
     */
    public static void addScannablePackages(String... packages) {
        if (ArrayUtils.isNotEmpty(packages)) {
            synchronized (SCANNABLE_PACKAGE_SET) {
                for (String pkg : packages) {
                    if (StringUtils.isNotBlank(pkg)) {
                        SCANNABLE_PACKAGE_SET.add(pkg.trim().toLowerCase());
                    }
                }
            }
        }
    }

    @Override
    public boolean isMatch(Object bean, String beanName, BeanDefinition beanDefinition) throws Throwable {
        if (SCANNABLE_PACKAGE_SET.isEmpty()) {
            // not exclude
            return false;
        }

        String className = bean.getClass().getName();
        for (String pkg : SCANNABLE_PACKAGE_SET) {
            if (className.startsWith(pkg)) {
                // not exclude
                return false;
            }
        }

        // exclude
        return true;
    }
}
