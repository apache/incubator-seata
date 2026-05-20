/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.common.json;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JSON deserialization allowlist manager.
 */
public class JsonAllowlistManager {

    private static final JsonAllowlistManager INSTANCE = new JsonAllowlistManager();

    /**
     * Built-in exact match allowlist
     */
    private final Set<String> builtinClasses = ConcurrentHashMap.newKeySet();

    /**
     * Built-in prefix match allowlist
     */
    private final Set<String> builtinPrefixes = ConcurrentHashMap.newKeySet();

    /**
     * User-defined exact match allowlist
     */
    private final Set<String> userClasses = ConcurrentHashMap.newKeySet();

    /**
     * User-defined prefix match allowlist
     */
    private final Set<String> userPrefixes = ConcurrentHashMap.newKeySet();

    /**
     * Check result cache (className -> allowed)
     */
    private final Map<String, Boolean> cache = new ConcurrentHashMap<>();

    private JsonAllowlistManager() {
        initBuiltinAllowlist();
    }

    public static JsonAllowlistManager getInstance() {
        return INSTANCE;
    }

    /**
     * Load user allowlist from configuration string
     */
    public void loadUserAllowlist(String config) {
        userClasses.clear();
        userPrefixes.clear();
        cache.clear();
        if (config == null || config.isEmpty()) {
            return;
        }
        for (String item : config.split(",")) {
            String trimmed = item.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (trimmed.endsWith(".")) {
                userPrefixes.add(trimmed);
            } else {
                userClasses.add(trimmed);
            }
        }
    }

    /**
     * Add a class to user allowlist programmatically
     */
    public void addUserClass(String className) {
        if (className != null && !className.isEmpty()) {
            userClasses.add(className);
            cache.remove(className);
        }
    }

    /**
     * Add a prefix to user allowlist programmatically
     */
    public void addUserPrefix(String prefix) {
        if (prefix != null && !prefix.isEmpty()) {
            userPrefixes.add(prefix);
            cache.clear();
        }
    }

    /**
     * Check if a class is allowed for deserialization
     */
    public boolean isAllowed(String className) {
        if (className == null) {
            return false;
        }
        return cache.computeIfAbsent(className, this::doCheck);
    }

    /**
     * Check if a class is allowed, throw SecurityException if not
     */
    public void checkClass(String className) {
        if (!isAllowed(className)) {
            throw new SecurityException("Class not in JSON deserialization allowlist: " + className
                    + ". Please add it to seata.json.allowlist configuration.");
        }
    }

    /**
     * Clear user allowlist and cache.
     */
    public void clearUserAllowlist() {
        userClasses.clear();
        userPrefixes.clear();
        cache.clear();
    }

    private boolean doCheck(String className) {
        if (isExactOrPrefixAllowed(className)) {
            return true;
        }
        if (isPrimitiveArrayDescriptor(className)) {
            return true;
        }
        String componentClassName = extractArrayComponentClassName(className);
        return componentClassName != null && isExactOrPrefixAllowed(componentClassName);
    }

    /**
     * Check if className is a multi-dimensional primitive array descriptor (e.g. "[[I", "[[Z").
     * Single-dimensional primitive arrays (e.g. "[I") are already in the builtin allowlist.
     */
    private boolean isPrimitiveArrayDescriptor(String className) {
        if (className == null || !className.startsWith("[[")) {
            return false;
        }
        String stripped = className;
        while (stripped.startsWith("[")) {
            stripped = stripped.substring(1);
        }
        return stripped.length() == 1 && PRIMITIVE_DESCRIPTORS.indexOf(stripped.charAt(0)) >= 0;
    }

    private boolean isExactOrPrefixAllowed(String className) {
        if (builtinClasses.contains(className)) {
            return true;
        }
        for (String prefix : builtinPrefixes) {
            if (className.startsWith(prefix)) {
                return true;
            }
        }
        if (userClasses.contains(className)) {
            return true;
        }
        for (String prefix : userPrefixes) {
            if (className.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static final String PRIMITIVE_DESCRIPTORS = "BCSIJFDZ";

    private String extractArrayComponentClassName(String className) {
        if (className == null || className.isEmpty()) {
            return null;
        }

        // Canonical name format: e.g. "int[][]", "String[]"
        String normalized = className;
        while (normalized.endsWith("[]")) {
            normalized = normalized.substring(0, normalized.length() - 2);
        }
        if (!normalized.equals(className)) {
            return normalized;
        }

        // JVM descriptor format: e.g. "[[I", "[Ljava.lang.String;"
        if (!normalized.startsWith("[")) {
            return null;
        }
        while (normalized.startsWith("[")) {
            normalized = normalized.substring(1);
        }
        // Primitive descriptor (e.g. "I", "Z") — handled by isPrimitiveArrayDescriptor
        if (normalized.length() == 1) {
            return null;
        }
        // Object descriptor: Lclassname;
        if (normalized.startsWith("L") && normalized.endsWith(";")) {
            return normalized.substring(1, normalized.length() - 1);
        }
        return null;
    }

    private void initBuiltinAllowlist() {

        builtinClasses.add("java.lang.Boolean");
        builtinClasses.add("java.lang.Byte");
        builtinClasses.add("java.lang.Character");
        builtinClasses.add("java.lang.Short");
        builtinClasses.add("java.lang.Integer");
        builtinClasses.add("java.lang.Long");
        builtinClasses.add("java.lang.Float");
        builtinClasses.add("java.lang.Double");
        builtinClasses.add("java.lang.String");
        builtinClasses.add("java.lang.Number");
        builtinClasses.add("java.math.BigDecimal");
        builtinClasses.add("java.math.BigInteger");

        // byte[]
        builtinClasses.add("[B");
        // char[]
        builtinClasses.add("[C");
        // short[]
        builtinClasses.add("[S");
        // int[]
        builtinClasses.add("[I");
        // long[]
        builtinClasses.add("[J");
        // float[]
        builtinClasses.add("[F");
        // double[]
        builtinClasses.add("[D");
        // boolean[]
        builtinClasses.add("[Z");
        builtinClasses.add("[Ljava.lang.String;");
        builtinClasses.add("[Ljava.lang.Object;");

        builtinClasses.add("java.util.Date");
        builtinClasses.add("java.util.Calendar");
        builtinClasses.add("java.util.GregorianCalendar");
        builtinClasses.add("java.sql.Date");
        builtinClasses.add("java.sql.Time");
        builtinClasses.add("java.sql.Timestamp");
        builtinClasses.add("java.time.LocalDateTime");
        builtinClasses.add("java.time.LocalDate");
        builtinClasses.add("java.time.LocalTime");
        builtinClasses.add("java.time.Instant");
        builtinClasses.add("java.time.Duration");
        builtinClasses.add("java.time.Period");
        builtinClasses.add("java.time.ZonedDateTime");
        builtinClasses.add("java.time.OffsetDateTime");
        builtinClasses.add("java.time.OffsetTime");
        builtinClasses.add("java.time.Year");
        builtinClasses.add("java.time.YearMonth");
        builtinClasses.add("java.time.MonthDay");
        builtinClasses.add("java.time.ZoneId");
        builtinClasses.add("java.time.ZoneOffset");

        builtinClasses.add("java.util.ArrayList");
        builtinClasses.add("java.util.LinkedList");
        builtinClasses.add("java.util.Vector");
        builtinClasses.add("java.util.Stack");
        builtinClasses.add("java.util.HashSet");
        builtinClasses.add("java.util.LinkedHashSet");
        builtinClasses.add("java.util.TreeSet");
        builtinClasses.add("java.util.HashMap");
        builtinClasses.add("java.util.LinkedHashMap");
        builtinClasses.add("java.util.TreeMap");
        builtinClasses.add("java.util.Hashtable");
        builtinClasses.add("java.util.Properties");
        builtinClasses.add("java.util.concurrent.ConcurrentHashMap");
        builtinClasses.add("java.util.concurrent.CopyOnWriteArrayList");
        builtinClasses.add("java.util.concurrent.CopyOnWriteArraySet");
        builtinClasses.add("java.util.concurrent.ConcurrentSkipListMap");
        builtinClasses.add("java.util.concurrent.ConcurrentSkipListSet");

        builtinClasses.add("java.util.UUID");
        builtinClasses.add("java.util.Locale");
        builtinClasses.add("java.util.Currency");
        builtinClasses.add("java.util.Optional");
        builtinClasses.add("java.net.URL");
        builtinClasses.add("java.net.URI");
        builtinClasses.add("java.io.File");
        builtinClasses.add("java.nio.file.Path");
        builtinClasses.add("java.util.regex.Pattern");

        builtinPrefixes.add("org.apache.seata.");
        builtinPrefixes.add("io.seata.");
    }
}
