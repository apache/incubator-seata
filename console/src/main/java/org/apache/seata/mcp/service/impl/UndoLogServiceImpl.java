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
package org.apache.seata.mcp.service.impl;

import org.apache.seata.mcp.entity.param.UndoLogParam;
import org.apache.seata.mcp.parser.FastjsonUndoLogParser;
import org.apache.seata.mcp.service.BusinessDataSourceService;
import org.apache.seata.mcp.service.UndoLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class UndoLogServiceImpl implements UndoLogService {

    @Autowired
    private BusinessDataSourceService dataSourceService;

    @Override
    public String queryAndAnalyzeUndoLog(UndoLogParam param) {
        // 1. First, query the undo_log data of the corresponding RM based on the parameters
        Map<String, List<byte[]>> undoLogInfo = dataSourceService.getUndoLogInfo(param);
        if (undoLogInfo.isEmpty()) {
            return "The corresponding undoLog data cannot be queried";
        }
        // 2. Then deserialize undoLogInfo to BranchUndoLog through FastJsonParser
        FastjsonUndoLogParser parser = new FastjsonUndoLogParser();
        List<String> result = new ArrayList<>();
        //        for (byte[] bytes : undoLogInfo) {
        //            result.add(parser.decode(bytes));
        //        }
        for (String context : undoLogInfo.keySet()) {
            List<byte[]> bytes = undoLogInfo.get(context);
            for (byte[] infos : bytes) {
                result.add(parser.decode(infos));
            }
        }
        return result.toString();
    }
}
