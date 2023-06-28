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
package io.seata.saga.statelang.parser;

/**
 *
 * Json Parser
 *
 * @author lorne.cl
 */
public interface JsonParser {

    /**
     * get Name
     *
     * @return the json parser name
     */
    String getName();

    /**
     * Object to Json string
     *
     * @param o the input object
     * @param prettyPrint is pretty and print
     * @return the json result
     */
    String toJsonString(Object o, boolean prettyPrint);


    /**
     * Check json use auto type boolean.
     *
     * @param json the json
     * @return the boolean
     */
    boolean useAutoType(String json);

    /**
     * Object to Json string
     * @param o the input object
     * @param ignoreAutoType is ignore auto type
     * @param prettyPrint is pretty and print
     * @return the json result
     */
    String toJsonString(Object o, boolean ignoreAutoType, boolean prettyPrint);

    /**
     * parse json string to Object
     *
     * @param json the parse input json
     * @param type the class type
     * @param <T> the object type
     * @return the parse result
     */
    <T> T parse(String json, Class<T> type, boolean ignoreAutoType);
}