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
package org.apache.seata.rm.datasource.undo;

import org.apache.seata.rm.datasource.undo.UndoLogParser;
import org.apache.seata.rm.datasource.undo.parser.FastjsonUndoLogParser;
import org.apache.seata.rm.datasource.undo.parser.JacksonUndoLogParser;
import org.apache.seata.rm.datasource.undo.parser.KryoUndoLogParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.apache.seata.common.loader.EnhancedServiceLoader;
import org.apache.seata.common.loader.EnhancedServiceNotFoundException;
import org.apache.seata.rm.datasource.undo.parser.ProtostuffUndoLogParser;


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
