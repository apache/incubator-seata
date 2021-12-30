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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.common.loader.EnhancedServiceNotFoundException;
import io.seata.rm.datasource.undo.parser.FastjsonUndoLogParser;
import io.seata.rm.datasource.undo.parser.FstUndoLogParser;
import io.seata.rm.datasource.undo.parser.JacksonUndoLogParser;
import io.seata.rm.datasource.undo.parser.KryoUndoLogParser;
import io.seata.rm.datasource.undo.parser.ProtostuffUndoLogParser;

/**
 * @author Geng Zhang
 */
class UndoLogParserProviderTest {

    @Test
    void testLoad(){
        UndoLogParser parser = EnhancedServiceLoader.load(UndoLogParser.class, "fastjson");
        Assertions.assertNotNull(parser);
        Assertions.assertTrue(parser instanceof FastjsonUndoLogParser);
        
        parser = EnhancedServiceLoader.load(UndoLogParser.class, "jackson");
        Assertions.assertNotNull(parser);
        Assertions.assertTrue(parser instanceof JacksonUndoLogParser);

        parser = EnhancedServiceLoader.load(UndoLogParser.class, "protostuff");
        Assertions.assertNotNull(parser);
        Assertions.assertTrue(parser instanceof ProtostuffUndoLogParser);
        
        parser = EnhancedServiceLoader.load(UndoLogParser.class, "fst");
        Assertions.assertNotNull(parser);
        Assertions.assertTrue(parser instanceof FstUndoLogParser);

        parser = EnhancedServiceLoader.load(UndoLogParser.class, "kryo");
        Assertions.assertNotNull(parser);
        Assertions.assertTrue(parser instanceof KryoUndoLogParser);

        try {
            EnhancedServiceLoader.load(UndoLogParser.class, "adadad");
            Assertions.fail();
        } catch (Exception e) {
            Assertions.assertTrue(e instanceof EnhancedServiceNotFoundException);
        }
    }
}