/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
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
package com.alibaba.fescar.rm.datasource.undo;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;

/**
 * The type Json based undo log parser.
 */
public class JSONBasedUndoLogParser implements UndoLogParser {

    @Override
    public String encode(BranchUndoLog branchUndoLog) {
        return JSON.toJSONString(branchUndoLog, SerializerFeature.WriteDateUseDateFormat);
    }

    @Override
    public BranchUndoLog decode(String text) {
        return JSON.parseObject(text, BranchUndoLog.class);
    }
}
