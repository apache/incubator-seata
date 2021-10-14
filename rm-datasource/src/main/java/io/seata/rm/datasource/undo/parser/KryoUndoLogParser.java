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

import com.esotericsoftware.kryo.Serializer;
import io.seata.common.executor.Initialize;
import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.common.loader.EnhancedServiceNotFoundException;
import io.seata.common.loader.LoadLevel;
import io.seata.common.util.CollectionUtils;
import io.seata.rm.datasource.undo.BranchUndoLog;
import io.seata.rm.datasource.undo.UndoLogParser;
import io.seata.rm.datasource.undo.parser.spi.KryoTypeSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * kryo serializer
 * @author jsbxyyx
 */
@LoadLevel(name = KryoUndoLogParser.NAME)
public class KryoUndoLogParser implements UndoLogParser, Initialize {

    private static final Logger LOGGER = LoggerFactory.getLogger(KryoUndoLogParser.class);

    public static final String NAME = "kryo";

    @Override
    public void init() {
        try {
            List<KryoTypeSerializer> serializers = EnhancedServiceLoader.loadAll(KryoTypeSerializer.class);
            if (CollectionUtils.isNotEmpty(serializers)) {
                for (KryoTypeSerializer typeSerializer : serializers) {
                    if (typeSerializer != null) {
                        Class type = typeSerializer.type();
                        Serializer ser = typeSerializer.serializer();
                        if (type != null) {
                            KryoSerializerFactory.getInstance().registerSerializer(type, ser);
                            LOGGER.info("kryo undo log parser load [{}].", typeSerializer.getClass().getName());
                        }
                    }
                }
            }
        } catch (EnhancedServiceNotFoundException e) {
            LOGGER.warn("KryoTypeSerializer not found children class.", e);
        }
    }

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
