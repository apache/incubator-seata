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
package io.seata.rm.datasource.undo.parser;

import io.seata.common.loader.LoadLevel;
import io.seata.rm.datasource.undo.BranchUndoLog;
import io.seata.rm.datasource.undo.UndoLogParser;

/**
 * kryo serializer
 * @author jsbxyyx
 */
@LoadLevel(name = KryoUndoLogParser.NAME)
public class KryoUndoLogParser implements UndoLogParser {

    public static final String NAME = "kryo";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public byte[] getDefaultContent() {
        KryoSerializer kryoSerializer = KryoSerializerFactory.getInstance().get();
        try {
            return kryoSerializer.serialize(new BranchUndoLog());
        } finally {
            KryoSerializerFactory.getInstance().returnKryo(kryoSerializer);
        }
    }

    @Override
    public byte[] encode(BranchUndoLog branchUndoLog) {
        KryoSerializer kryoSerializer = KryoSerializerFactory.getInstance().get();
        try {
            return kryoSerializer.serialize(branchUndoLog);
        } finally {
            KryoSerializerFactory.getInstance().returnKryo(kryoSerializer);
        }
    }

    @Override
    public BranchUndoLog decode(byte[] bytes) {
        KryoSerializer kryoSerializer = KryoSerializerFactory.getInstance().get();
        try {
            return kryoSerializer.deserialize(bytes);
        } finally {
            KryoSerializerFactory.getInstance().returnKryo(kryoSerializer);
        }
    }

}
