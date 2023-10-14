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
package io.seata.core.serializer;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import io.seata.core.exception.TransactionExceptionCode;
import io.seata.core.model.BranchStatus;
import io.seata.core.model.BranchType;
import io.seata.core.model.GlobalStatus;
import io.seata.core.protocol.ResultCode;

/**
 * Serializer Security Registry
 * @author funkye
 */
public class SerializerSecurityRegistry {
    private static final Set<Class<?>> ALLOW_CLAZZ_SET = new HashSet<>();

    private static final Set<String> ALLOW_CLAZZ_PATTERN = new HashSet<>();

    private static final Set<String> DENY_CLAZZ_PATTERN = new HashSet<>();

    private static final String CLASS_POSTFIX = ".class";

    private static final String ABSTRACT_CLASS_ID = "Abstract";

    private static final String REQUEST_CLASS_ID = "Request";

    private static final String RESPONSE_CLASS_ID = "Response";

    private static final String MESSAGE_CLASS_ID = "Message";

    static {
        ALLOW_CLAZZ_SET.addAll(Arrays.asList(getBasicClassType()));
        ALLOW_CLAZZ_SET.addAll(Arrays.asList(getCollectionClassType()));
        ALLOW_CLAZZ_SET.addAll(getProtocolType());
        ALLOW_CLAZZ_SET.addAll(Arrays.asList(getProtocolInnerFields()));

        for (Class<?> clazz : ALLOW_CLAZZ_SET) {
            ALLOW_CLAZZ_PATTERN.add(clazz.getCanonicalName());
        }
        ALLOW_CLAZZ_PATTERN.add(getSeataClassPattern());

        DENY_CLAZZ_PATTERN.addAll(Arrays.asList(getDenyClassPatternList()));
    }

    public static Set<Class<?>> getAllowClassType() {
        return Collections.unmodifiableSet(ALLOW_CLAZZ_SET);
    }

    public static Set<String> getAllowClassPattern() {
        return Collections.unmodifiableSet(ALLOW_CLAZZ_PATTERN);
    }

    public static Set<String> getDenyClassPattern() {
        return Collections.unmodifiableSet(DENY_CLAZZ_PATTERN);
    }

    private static Class<?>[] getBasicClassType() {
        return new Class[] {Boolean.class, Byte.class, Character.class, Double.class, Float.class, Integer.class,
            Long.class, Short.class, Number.class, Class.class, String.class};
    }

    private static Class<?>[] getCollectionClassType() {
        return new Class[] {ArrayList.class, LinkedList.class, HashSet.class,
            LinkedHashSet.class, TreeSet.class, HashMap.class, LinkedHashMap.class, TreeMap.class};
    }

    private static String getSeataClassPattern() {
        return "io.seata.*";
    }

    private static String[] getDenyClassPatternList() {
        return new String[] {"javax.naming.InitialContext", "javax.net.ssl.*", "com.unboundid.ldap.*"};
    }

    private static Set<Class<?>> getProtocolType() {
        Enumeration<URL> packageDir = null;
        String packageName = "io.seata.core.protocol";
        Set<Class<?>> classNameSet = new HashSet<>();
        try {
            packageDir = Thread.currentThread().getContextClassLoader().getResources(packageName.replace(".", "/"));
        } catch (IOException ignore) {
        }
        while (packageDir.hasMoreElements()) {
            String filePath = packageDir.nextElement().getFile();
            findProtocolClassByPackage(filePath, packageName, classNameSet);
        }
        return classNameSet;
    }

    private static void findProtocolClassByPackage(String classPath, String rootPackageName, Set classNameSet) {
        File file = new File(classPath);
        if (!file.exists()) {
            return;
        }
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (null == files) {
                return;
            }
            for (File path : files) {
                if (path.isDirectory()) {
                    findProtocolClassByPackage(path.getAbsolutePath(), rootPackageName + "." + path.getName(),
                        classNameSet);
                } else {
                    findProtocolClassByPackage(path.getAbsolutePath(), rootPackageName, classNameSet);
                }
            }
        } else {
            if (matchProtocol(file.getName())) {
                String className = file.getName().substring(0, file.getName().length() - CLASS_POSTFIX.length());
                try {
                    classNameSet.add(
                        Thread.currentThread().getContextClassLoader().loadClass(rootPackageName + '.' + className));
                } catch (ClassNotFoundException ignore) {
                    //ignore interface
                }
            }
        }
    }

    private static boolean matchProtocol(String fileName) {
        if (!fileName.endsWith(CLASS_POSTFIX)) {
            return false;
        }
        fileName = fileName.replace(CLASS_POSTFIX, "");
        if (fileName.startsWith(ABSTRACT_CLASS_ID)) {
            return false;
        }
        if (fileName.contains(REQUEST_CLASS_ID) || fileName.contains(RESPONSE_CLASS_ID) || fileName.endsWith(MESSAGE_CLASS_ID)) {
            return true;
        }
        return false;
    }

    private static Class<?>[] getProtocolInnerFields() {
        return new Class<?>[] {ResultCode.class, GlobalStatus.class, BranchStatus.class, BranchType.class, TransactionExceptionCode.class};
    }
}
