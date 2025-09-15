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
package org.apache.seata.mcp.undo.parser;

import org.apache.seata.common.Constants;

/**
 * The type Json based undo log parser.
 */
public class UndoLogParser {
    public String decode(String context, byte[] bytes) {
        if (!context.contains("fastjson") && !context.contains("jackson"))
            return "Only RollBackInfo parsing in Json format is supported, and the RollBackInfo serialization tool currently recorded is: "
                    + context;
        return new String(bytes, Constants.DEFAULT_CHARSET);
    }
}
