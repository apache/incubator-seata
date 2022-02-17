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

import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.rm.datasource.undo.BaseUndoLogParserTest;
import io.seata.rm.datasource.undo.UndoLogParser;

/**
 * @author jsbxyyx
 */
//@DisabledOnJre(JRE.JAVA_17) // `kryo` does not supported java17
public class KryoUndoLogParserTest extends BaseUndoLogParserTest {

    KryoUndoLogParser parser = (KryoUndoLogParser) EnhancedServiceLoader.load(UndoLogParser.class, KryoUndoLogParser.NAME);

    @Override
    public UndoLogParser getParser() {
        return parser;
    }


}
