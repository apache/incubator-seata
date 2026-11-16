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

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link MergeNativeImageMetadata#mergeObjectNodes}.
 *
 * <p>Each test constructs inline source and target JSON strings and
 * verifies that after merging, the resulting target matches the expected
 * shape — ensuring merge rules are applied correctly:
 *
 * <ul>
 *   <li>Top-level keys absent in target are added.</li>
 *   <li>Array elements at matching indices are merged recursively.</li>
 *   <li>Extra source array elements are appended.</li>
 *   <li>Object-valued keys are merged deeply.</li>
 *   <li>Existing target keys keep their values unchanged.</li>
 * </ul>
 */
class MergeNativeImageMetadataTests {

    /** Shared ObjectMapper instance for reading inline JSON. */
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * When target is empty, all top-level keys from source should be added.
     * Verifies that the {@code "resources"} key is copied into an empty target.
     */
    @Test
    void addResources() {

        String source =
                """
                {
                  "resources" : [ {
                    "glob" : "META-INF/build-info.properties"
                  }, {
                    "glob" : "META-INF/resources/api/v1/console/globalLock/query"
                  } ]
                }
                """;
        String target = """
                {
                }
                """;

        JsonNode sourceNode = objectMapper.readTree(source);
        JsonNode targetNode = objectMapper.readTree(target);

        MergeNativeImageMetadata.mergeObjectNodes(sourceNode, targetNode);

        assertEquals(sourceNode, targetNode);
    }

    /**
     * When target already contains a subset of resource entries, only the
     * missing entry should be appended. The existing entry is preserved
     * and the extra source entry is added at the end of the array.
     */
    @Test
    void addResourcesGlob() {

        String source =
                """
                {
                  "resources" : [ {
                    "glob" : "META-INF/build-info.properties"
                  }, {
                    "glob" : "META-INF/resources/api/v1/console/globalLock/query"
                  } ]
                }
                """;
        String target =
                """
                {
                  "resources" : [ {
                    "glob" : "META-INF/build-info.properties"
                  } ]
                }
                """;

        JsonNode sourceNode = objectMapper.readTree(source);
        JsonNode targetNode = objectMapper.readTree(target);

        MergeNativeImageMetadata.mergeObjectNodes(sourceNode, targetNode);

        assertEquals(sourceNode, targetNode);
    }

    /**
     * When source adds an element with extra fields (e.g. {@code "module"}
     * alongside {@code "glob"}), the existing matching-index elements are
     * merged recursively and the new element is appended.
     */
    @Test
    void addResourcesGlobModule() {

        String source =
                """
                {
                   "resources" : [ {
                     "glob" : "META-INF/build-info.properties"
                   }, {
                     "glob" : "META-INF/resources/api/v1/console/globalLock/query"
                   }, {
                     "module" : "jdk.jfr",
                     "glob" : "jdk/jfr/internal/query/view.ini"
                   } ]
                }
                """;
        String target =
                """
                {
                   "resources" : [ {
                     "glob" : "META-INF/build-info.properties"
                   }, {
                     "glob" : "META-INF/resources/api/v1/console/globalLock/query"
                   } ]
                }
                """;

        JsonNode sourceNode = objectMapper.readTree(source);
        JsonNode targetNode = objectMapper.readTree(target);

        MergeNativeImageMetadata.mergeObjectNodes(sourceNode, targetNode);

        assertEquals(sourceNode, targetNode);
    }

    /**
     * When target is empty, all reflection entries from source should be
     * added — including type entries with and without methods.
     */
    @Test
    void addReflection() {

        String source =
                """
                {
                   "reflection" : [ {
                     "type" : "apple.security.AppleProvider",
                     "methods" : [ {
                       "name" : "<init>",
                       "parameterTypes" : [ ]
                     } ]
                   }, {
                     "type" : "boolean"
                   } ]
                 }
                """;
        String target = """
                {
                }
                """;

        JsonNode sourceNode = objectMapper.readTree(source);
        JsonNode targetNode = objectMapper.readTree(target);

        MergeNativeImageMetadata.mergeObjectNodes(sourceNode, targetNode);

        assertEquals(sourceNode, targetNode);
    }

    /**
     * When target already has a subset of reflection type entries, the
     * extra type from source should be appended while existing entries
     * remain unchanged.
     */
    @Test
    void addReflectionType() {

        String source =
                """
                {
                   "reflection" : [ {
                     "type" : "apple.security.AppleProvider",
                     "methods" : [ {
                       "name" : "<init>",
                       "parameterTypes" : [ ]
                     } ]
                   }, {
                     "type" : "boolean"
                   }, {
                     "type" : "ch.qos.logback.core.rolling.TimeBasedRollingPolicy"
                   } ]
                 }
                """;
        String target =
                """
                {
                   "reflection" : [ {
                     "type" : "apple.security.AppleProvider",
                     "methods" : [ {
                       "name" : "<init>",
                       "parameterTypes" : [ ]
                     } ]
                   }, {
                     "type" : "boolean"
                   } ]
                }
                """;

        JsonNode sourceNode = objectMapper.readTree(source);
        JsonNode targetNode = objectMapper.readTree(target);

        MergeNativeImageMetadata.mergeObjectNodes(sourceNode, targetNode);

        assertEquals(sourceNode, targetNode);
    }

    /**
     * When an existing reflection entry has a subset of methods, the
     * extra methods from source should be appended to that entry's
     * {@code "methods"} array — demonstrating deep array merge at
     * matching indices.
     */
    @Test
    void addReflectionTypeMethods() {

        String source =
                """
                {
                   "reflection" : [ {
                     "type" : "apple.security.AppleProvider",
                     "methods" : [ {
                       "name" : "<init>",
                       "parameterTypes" : [ ]
                     } ]
                   }, {
                     "type" : "ch.qos.logback.classic.AsyncAppender",
                     "methods" : [ {
                       "name" : "<init>",
                       "parameterTypes" : [ ]
                     }, {
                       "name" : "setIncludeCallerData",
                       "parameterTypes" : [ "boolean" ]
                     } ]
                   } ]
                }
                """;
        String target =
                """
                {
                   "reflection" : [ {
                     "type" : "apple.security.AppleProvider",
                     "methods" : [ {
                       "name" : "<init>",
                       "parameterTypes" : [ ]
                     } ]
                   }, {
                     "type" : "ch.qos.logback.classic.AsyncAppender",
                     "methods" : [ {
                       "name" : "<init>",
                       "parameterTypes" : [ ]
                     } ]
                   } ]
                }
                """;

        JsonNode sourceNode = objectMapper.readTree(source);
        JsonNode targetNode = objectMapper.readTree(target);

        MergeNativeImageMetadata.mergeObjectNodes(sourceNode, targetNode);

        assertEquals(sourceNode, targetNode);
    }

    /**
     * When an existing reflection entry is missing {@code "fields"},
     * the fields from source should be added to that entry — verifying
     * that new object keys inside array elements are merged deeply.
     */
    @Test
    void addReflectionTypeFields() {

        String source =
                """
                {
                  "reflection" : [ {
                    "type" : "apple.security.AppleProvider",
                    "methods" : [ {
                      "name" : "<init>",
                      "parameterTypes" : [ ]
                    } ]
                  }, {
                    "type" : "org.apache.seata.console.config.WebSecurityConfig",
                    "methods" : [ {
                      "name" : "<init>",
                      "parameterTypes" : [ ]
                    } ],
                    "fields" : [ {
                      "name" : "csrfIgnoreUrls"
                    } ]
                  } ]
                }
                """;
        String target =
                """
                {
                  "reflection" : [ {
                    "type" : "apple.security.AppleProvider",
                    "methods" : [ {
                      "name" : "<init>",
                      "parameterTypes" : [ ]
                    } ]
                  }, {
                    "type" : "org.apache.seata.console.config.WebSecurityConfig",
                    "methods" : [ {
                      "name" : "<init>",
                      "parameterTypes" : [ ]
                    } ]
                  } ]
                }
                """;

        JsonNode sourceNode = objectMapper.readTree(source);
        JsonNode targetNode = objectMapper.readTree(target);

        MergeNativeImageMetadata.mergeObjectNodes(sourceNode, targetNode);

        assertEquals(sourceNode, targetNode);
    }
}
