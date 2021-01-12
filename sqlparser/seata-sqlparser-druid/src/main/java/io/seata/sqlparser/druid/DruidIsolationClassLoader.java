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
package io.seata.sqlparser.druid;

import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.List;

/**
 * Used for druid isolation.
 *
 * @author ggndnn
 */
class DruidIsolationClassLoader extends URLClassLoader {
    private final static String[] DRUID_CLASS_PREFIX = new String[]{"com.alibaba.druid.", "io.seata.sqlparser.druid."};

    private final static DruidIsolationClassLoader INSTANCE = new DruidIsolationClassLoader(DefaultDruidLoader.get());

    DruidIsolationClassLoader(DruidLoader druidLoader) {
        super(getDruidUrls(druidLoader), DruidIsolationClassLoader.class.getClassLoader());
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        for (String prefix : DRUID_CLASS_PREFIX) {
            if (name.startsWith(prefix)) {
                return loadInternalClass(name, resolve);
            }
        }
        return super.loadClass(name, resolve);
    }

    private Class<?> loadInternalClass(String name, boolean resolve) throws ClassNotFoundException {
        Class<?> c;
        synchronized (getClassLoadingLock(name)) {
            c = findLoadedClass(name);
            if (c == null) {
                c = findClass(name);
            }
        }
        if (c == null) {
            throw new ClassNotFoundException(name);
        }
        if (resolve) {
            resolveClass(c);
        }
        return c;
    }

    private static URL[] getDruidUrls(DruidLoader druidLoader) {
        List<URL> urls = new ArrayList<>();
        urls.add(findClassLocation(DruidIsolationClassLoader.class));
        urls.add(druidLoader.getEmbeddedDruidLocation());
        return urls.toArray(new URL[0]);
    }

    private static URL findClassLocation(Class<?> clazz) {
        CodeSource cs = clazz.getProtectionDomain().getCodeSource();
        if (cs == null) {
            throw new IllegalStateException("Not a normal druid startup environment");
        }
        return cs.getLocation();
    }

    static DruidIsolationClassLoader get() {
        return INSTANCE;
    }
}
