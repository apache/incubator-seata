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
package io.seata.rm.datasource.undo;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import io.seata.common.Constants;
import io.seata.common.loader.LoadLevel;

/**
 * The type Json based undo log parser.
 *
 * @author sharajava
 */
@LoadLevel(name = "fastjson")
public class JSONBasedUndoLogParser implements UndoLogParser {

    @Override
    public byte[] encode(BranchUndoLog branchUndoLog) {
        String json = JSON.toJSONString(branchUndoLog, SerializerFeature.WriteDateUseDateFormat);
        return json.getBytes(Constants.DEFAULT_CHARSET);
    }

    @Override
    public BranchUndoLog decode(byte[] bytes) {
        String text = new String(bytes, Constants.DEFAULT_CHARSET);
        return JSON.parseObject(text, BranchUndoLog.class);
    }
}
