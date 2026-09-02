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
package org.apache.seata.metadata;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Utility for merging GraalVM native image reachability metadata JSON files.
 *
 * <p>The merge is source-into-target with target-preserving semantics:
 * existing keys in the target are never overwritten; only missing keys
 * are added from the source. This ensures that manually curated metadata
 * is not lost when merging automatically generated entries.
 *
 * <p>Typical usage involves running the application with the GraalVM native
 * image agent, which produces a {@code reachability-metadata.json} file
 * under {@code target/native-image-config/}. That file is merged into the
 * canonical metadata file under
 * {@code src/main/resources/META-INF/native-image/} via
 * {@link #mergeObjectNodes(JsonNode, JsonNode)}.
 *
 * <h3>Array Merge Strategy</h3>
 * Array elements are matched by natural identity keys (not by index):
 * <ul>
 *   <li><b>type</b> — for reflection, JNI, serialization, and
 *       predefined-classes entries (optional: condition).</li>
 *   <li><b>glob</b> — for resource entries (optional: module).</li>
 *   <li><b>bundle</b> — for resource bundle entries.</li>
 *   <li><b>name</b> — for method and field entries
 *       (optional: parameterTypes for methods).</li>
 * </ul>
 * Matching elements are deep-merged recursively; unmatched source
 * elements are appended to the target array. Elements without a
 * recognizable key are always appended.
 */
public class MergeNativeImageMetadata {

    /** Shared ObjectMapper instance. */
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Recursively merges fields from {@code source} into {@code target}
     * without overwriting existing {@code target} fields. A key is only
     * written from {@code source} when it does not already exist in
     * {@code target}.
     *
     * <h3>Merge rules</h3>
     * <ul>
     *   <li><b>Key missing in target</b> — copied from source as-is.</li>
     *   <li><b>Both values are objects</b> — merged recursively.</li>
     *   <li><b>Both values are arrays</b> — elements are matched by
     *       identity key (e.g. {@code type}, {@code glob}, {@code name}),
     *       then merged recursively; unmatched source elements are
     *       appended.</li>
     *   <li><b>Otherwise</b> — target value is kept unchanged (source
     *       value is ignored).</li>
     * </ul>
     *
     * @param source the generated metadata to merge from
     * @param target the canonical metadata to merge into (mutated in place)
     */
    public static void mergeObjectNodes(JsonNode source, JsonNode target) {
        if (!source.isObject() || !target.isObject()) {
            return;
        }
        for (Map.Entry<String, JsonNode> entry : source.properties()) {
            String key = entry.getKey();
            JsonNode sourceValue = entry.getValue();
            JsonNode targetValue = target.get(key);
            if (targetValue == null) {
                // Key is absent in target — copy from source directly.
                ((ObjectNode) target).set(key, sourceValue);
            } else if (sourceValue.isObject() && targetValue.isObject()) {
                // Both are objects — merge recursively.
                mergeObjectNodes(sourceValue, targetValue);
            } else if (sourceValue.isArray() && targetValue.isArray()) {
                // Both are arrays — merge by identity key, then append unmatched.
                mergeArrayNodes(sourceValue, (ArrayNode) targetValue);
            }
            // Otherwise the target already has a value and the types
            // differ — keep the target value unchanged.
        }
    }

    /**
     * Merges array elements from {@code sourceArray} into {@code targetArray}
     * using key-based matching.
     *
     * <p>Each element in the target array is indexed by its identity key
     * (see {@link #getElementKey(JsonNode)}). Source elements with a
     * matching key are deep-merged into the corresponding target element;
     * source elements without a match (or without a recognizable key) are
     * appended to the target array.
     *
     * @param sourceArray the source array (from generated metadata)
     * @param targetArray the target array (canonical, mutated in place)
     */
    private static void mergeArrayNodes(JsonNode sourceArray, ArrayNode targetArray) {
        // Build key → index map for target elements (first occurrence wins).
        Map<String, Integer> targetIndex = new LinkedHashMap<>();
        for (int i = 0; i < targetArray.size(); i++) {
            String key = getElementKey(targetArray.get(i));
            if (key != null && !targetIndex.containsKey(key)) {
                targetIndex.put(key, i);
            }
        }

        // Process each source element: match by key or append.
        for (JsonNode sourceElement : sourceArray) {
            String sourceKey = getElementKey(sourceElement);
            if (sourceKey != null && targetIndex.containsKey(sourceKey)) {
                // Match found — deep-merge source into the matching target element.
                int targetIdx = targetIndex.get(sourceKey);
                mergeObjectNodes(sourceElement, targetArray.get(targetIdx));
            } else if (!sourceElement.isObject()) {
                // Scalar value (string, number, boolean) or nested array —
                // append only if not already present to avoid duplication.
                if (!containsNode(targetArray, sourceElement)) {
                    targetArray.add(sourceElement);
                }
            } else if (sourceKey != null) {
                // Object with a key not yet in target — append and register
                // the key so subsequent source elements with the same key
                // merge into this one instead of being appended as duplicates.
                targetArray.add(sourceElement);
                targetIndex.put(sourceKey, targetArray.size() - 1);
            } else {
                // Object without a recognizable key — append only if not
                // already present to avoid duplication.
                if (!containsNode(targetArray, sourceElement)) {
                    targetArray.add(sourceElement);
                }
            }
        }
    }

    /**
     * Checks whether {@code array} contains an element that is
     * {@link JsonNode#equals(Object) equal} to {@code value}.
     */
    private static boolean containsNode(ArrayNode array, JsonNode value) {
        for (JsonNode element : array) {
            if (element.equals(value)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Extracts a natural identity key from an array element.
     *
     * <p>The key is built from fields commonly used to identify entries
     * in GraalVM reachability metadata:
     *
     * <ul>
     *   <li>{@code type} — primary key for reflection, JNI,
     *       serialization, and predefined-classes entries.
     *       If {@code condition} is also present, it is included.</li>
     *   <li>{@code glob} — primary key for resource includes/excludes.
     *       If {@code module} is also present, it is included.</li>
     *   <li>{@code bundle} — primary key for resource bundle entries.</li>
     *   <li>{@code name} — primary key for method and field entries.
     *       If {@code parameterTypes} is also present (for methods),
     *       it is included.</li>
     * </ul>
     *
     * @param element an array element (expected to be a JSON object)
     * @return the identity key string, or {@code null} if no
     *         recognizable key fields are present
     */
    private static String getElementKey(JsonNode element) {
        if (!element.isObject()) {
            return null;
        }
        // Reflection / JNI / serialization / predefined-classes entries.
        // The "type" field is normally a string but can also be a complex
        // value for proxy metadata ({"proxy": [...]}) or lambda metadata
        // ({"lambda": {...}}). Use toString() for non-textual values.
        if (element.has("type")) {
            StringBuilder sb = new StringBuilder("type:");
            sb.append(normalizeType(nodeText(element.get("type"))));
            if (element.has("condition")) {
                sb.append("|condition:");
                sb.append(nodeText(element.get("condition")));
            }
            return sb.toString();
        }
        // Resource entries
        if (element.has("glob")) {
            StringBuilder sb = new StringBuilder("glob:");
            sb.append(nodeText(element.get("glob")));
            if (element.has("module")) {
                sb.append("|module:");
                sb.append(nodeText(element.get("module")));
            }
            return sb.toString();
        }
        // Bundle entries (resource bundles, e.g. {"bundle": "com.example.LocalStrings"})
        if (element.has("bundle")) {
            return "bundle:" + nodeText(element.get("bundle"));
        }
        // Method / field entries
        if (element.has("name")) {
            StringBuilder sb = new StringBuilder("name:");
            sb.append(nodeText(element.get("name")));
            if (element.has("parameterTypes")) {
                sb.append("|params:");
                sb.append(element.get("parameterTypes").toString());
            }
            return sb.toString();
        }
        return null;
    }

    /**
     * Returns the text representation of a JsonNode.
     *
     * <p>For text nodes, returns the string value via {@link JsonNode#asString()}.
     * For non-text nodes (objects, arrays), falls back to
     * {@link JsonNode#toString()} to produce a stable key.
     */
    private static String nodeText(JsonNode node) {
        if (node.isString()) {
            return node.asString();
        }
        return node.toString();
    }

    /**
     * Pattern matching Guice-generated {@code FastClass} type names with a
     * variable numeric hash suffix, e.g.
     * {@code com.example.MyClass$$FastClassByGuice$$1231617}.
     *
     * <p>The numeric component is a hash derived from the base class method
     * signatures; it can change between builds. Stripping it during key
     * computation prevents duplicate entries from accumulating in the
     * canonical metadata file.
     */
    private static final Pattern FAST_CLASS_BY_GUICE_SUFFIX = Pattern.compile("\\$\\$FastClassByGuice\\$\\$\\d+$");

    /**
     * Normalizes a reflection type name for stable identity-key matching by
     * stripping the variable numeric suffix from Guice FastClass names.
     *
     * <p>Example:
     * {@code com.ctrip.framework.apollo.spring.config.ConfigPropertySourceFactory$$FastClassByGuice$$1231617}
     * → {@code com.ctrip.framework.apollo.spring.config.ConfigPropertySourceFactory$$FastClassByGuice$$}
     *
     * <p>Type names that do not match the pattern are returned unchanged.
     *
     * @param typeValue the raw type name from a metadata entry
     * @return the normalized type name for key matching
     */
    static String normalizeType(String typeValue) {
        return FAST_CLASS_BY_GUICE_SUFFIX.matcher(typeValue).replaceAll("\\$\\$FastClassByGuice\\$\\$");
    }
}
