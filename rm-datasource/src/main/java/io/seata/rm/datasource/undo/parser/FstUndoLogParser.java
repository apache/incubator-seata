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

import io.seata.common.executor.Initialize;
import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.common.loader.EnhancedServiceNotFoundException;
import io.seata.common.loader.LoadLevel;
import io.seata.common.util.CollectionUtils;
import io.seata.rm.datasource.undo.BranchUndoLog;
import io.seata.rm.datasource.undo.UndoLogParser;
import io.seata.rm.datasource.undo.parser.spi.FstSerializer;
import org.nustaq.serialization.FSTObjectSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * fst serializer
 * @author funkye
 */
@LoadLevel(name = FstUndoLogParser.NAME)
public class FstUndoLogParser implements UndoLogParser, Initialize {

    private static final Logger LOGGER = LoggerFactory.getLogger(FstUndoLogParser.class);

    public static final String NAME = "fst";

    private FstSerializerFactory fstFactory = FstSerializerFactory.getDefaultFactory();

    @Override
    public void init() {
        try {
            List<FstSerializer> serializers = EnhancedServiceLoader.loadAll(FstSerializer.class);
            if (CollectionUtils.isNotEmpty(serializers)) {
                for (FstSerializer serializer : serializers) {
                    if (serializer != null) {
                        Class type = serializer.type();
                        FSTObjectSerializer ser = serializer.ser();
                        boolean alsoForAllSubclasses = serializer.alsoForAllSubclasses();
                        if (type != null && ser != null) {
                            fstFactory.registerSerializer(type, ser, alsoForAllSubclasses);
                            LOGGER.info("fst undo log parser load [{}].", serializer.getClass().getName());
                        }
                    }
                }
            }
        } catch (EnhancedServiceNotFoundException e) {
            LOGGER.warn("FstSerializer not found children class.", e);
        }
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public byte[] getDefaultContent() {
        return fstFactory.serialize(new BranchUndoLog());
    }

    @Override
    public byte[] encode(BranchUndoLog branchUndoLog) {
        return fstFactory.serialize(branchUndoLog);
    }

    @Override
    public BranchUndoLog decode(byte[] bytes) {
        return fstFactory.deserialize(bytes);
    }

}
