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
package org.apache.seata.rm.datasource.undo.parser;

import org.apache.fury.Fury;
import org.apache.fury.ThreadLocalFury;
import org.apache.fury.ThreadSafeFury;
import org.apache.fury.config.CompatibleMode;
import org.apache.fury.config.Language;
import org.apache.seata.common.executor.Initialize;
import org.apache.seata.common.loader.LoadLevel;
import org.apache.seata.rm.datasource.undo.BranchUndoLog;
import org.apache.seata.rm.datasource.undo.UndoLogParser;

@LoadLevel(name = FuryUndoLogParser.NAME)
public class FuryUndoLogParser implements UndoLogParser, Initialize {
    public static final String NAME = "fury";

    private static final ThreadSafeFury FURY = new ThreadLocalFury(classLoader -> Fury.builder()
            .withLanguage(Language.JAVA)
            // In JAVA mode, classes cannot be registered by tag, and the different registration order between the server and the client will cause deserialization failure
            // In XLANG cross-language mode has problems with Java class serialization, such as enum classes [https://github.com/apache/fury/issues/1644].
            .requireClassRegistration(false)
            //enable reference tracking for shared/circular reference.
            .withRefTracking(true)
            .withClassLoader(classLoader)
            .withCompatibleMode(CompatibleMode.COMPATIBLE)
            .build());
    @Override
    public void init() {
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public byte[] getDefaultContent() {
        return encode(new BranchUndoLog());
    }

    @Override
    public byte[] encode(BranchUndoLog branchUndoLog) {
        return FURY.serializeJavaObject(branchUndoLog);
    }

    @Override
    public BranchUndoLog decode(byte[] bytes) {
        return FURY.deserializeJavaObject(bytes, BranchUndoLog.class);
    }

}
