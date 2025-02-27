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
package org.apache.seata.common;

public interface NamingServerConstants {
    /**
     * The constant HTTP_PREFIX
     */
    String HTTP_PREFIX = "http://";

    /**
     * The constant HTTP_ADD_GROUP_SUFFIX
     */
    String HTTP_ADD_GROUP_SUFFIX = "/vgroup/v1/addVGroup?";

    /**
     * The constant CONSTANT_UNIT
     */
    String CONSTANT_UNIT = "unit";

    /**
     * The constant CONSTANT_GROUP
     */
    String CONSTANT_GROUP = "vGroup";

    /**
     * The constant HTTP_REMOVE_GROUP_SUFFIX
     */
    String HTTP_REMOVE_GROUP_SUFFIX = "/vgroup/v1/removeVGroup?";

    /**
     * The constant IP_PORT_SPLIT_CHAR
     */
    String IP_PORT_SPLIT_CHAR = ":";

    /**
     * The constant DEFAULT_VGROUP_MAPPING
     */
    String DEFAULT_VGROUP_MAPPING = "vgroup_table";

}
