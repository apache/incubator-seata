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
package io.seata.spring.schema;

import java.lang.reflect.Method;
import java.util.Set;

import io.seata.common.exception.ShouldNeverHappenException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.util.PatternMatchUtils;

/**
 * The type gtx target scanner
 *
 * @author xingfudeshi@gmail.com
 */
public class GtxTargetScanner implements ResourceLoaderAware {
    private static final Logger LOGGER = LoggerFactory.getLogger(GtxTargetScanner.class);
    private static final String PATTERN_CLASS = "@CLASS";
    private static final String PATTERN_METHOD = "@METHOD";
    private final Set<GtxConfig> gtxConfigs;

    public GtxTargetScanner(Set<GtxConfig> gtxConfigs) {
        this.gtxConfigs = gtxConfigs;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        try {
            ResourcePatternResolver resolver = ResourcePatternUtils.getResourcePatternResolver(resourceLoader);
            MetadataReaderFactory metaReader = new CachingMetadataReaderFactory(resourceLoader);

            for (GtxConfig gtxConfig : this.gtxConfigs) {
                Resource[] resources = resolver.getResources("classpath*:" + formatPackageName(gtxConfig.getScanPackage()) + "/*.class");
                for (Resource r : resources) {
                    MetadataReader reader = metaReader.getMetadataReader(r);
                    String className = reader.getClassMetadata().getClassName();

                    String pattern = gtxConfig.getPattern();
                    if (StringUtils.startsWith(pattern, PATTERN_CLASS)) {
                        String regex = StringUtils.split(pattern, ":")[1];
                        if (PatternMatchUtils.simpleMatch(regex, className)) {
                            GtxTarget gtxTarget = new GtxTarget();
                            gtxTarget.setGtxConfig(gtxConfig);
                            gtxTarget.setGtxTargetType(GtxTargetType.CLASS);
                            gtxTarget.setTargetName(className);
                            GtxTargetHolder.INSTANCE.add(gtxTarget);
                        }


                    } else if (StringUtils.startsWith(pattern, PATTERN_METHOD)) {
                        String regex = StringUtils.split(pattern, ":")[1];
                        Class<?> targetClass = Class.forName(className);
                        Method[] methods = targetClass.getMethods();
                        for (Method method : methods) {
                            String name = method.getName();
                            if (PatternMatchUtils.simpleMatch(regex, name)) {
                                GtxTarget gtxTarget = new GtxTarget();
                                gtxTarget.setGtxConfig(gtxConfig);
                                gtxTarget.setGtxTargetType(GtxTargetType.METHOD);
                                gtxTarget.setTargetName(name);
                                GtxTargetHolder.INSTANCE.add(gtxTarget);
                            }
                        }


                    } else {
                        throw new ShouldNeverHappenException("pattern [" + pattern + "] is not supported yet");
                    }
                }
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * format package name
     *
     * @param packageName
     * @return java.lang.String
     * @author xingfudeshi@gmail.com
     */
    private static String formatPackageName(String packageName) {
        if (StringUtils.isBlank(packageName)) {
            return StringUtils.EMPTY;
        }
        return packageName.replace(".", "/");
    }

}
