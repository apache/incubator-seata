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
<<<<<<<< HEAD:rm-datasource/src/test/java/io/seata/rm/datasource/undo/polardbx/keyword/PolarDBXEscapeHandlerTest.java
package io.seata.rm.datasource.undo.polardbx.keyword;
========

package io.seata.common.loader;
>>>>>>>> upstream/2.x:common/src/test/java/io/seata/common/loader/ExtensionDefinitionTest.java

import io.seata.sqlparser.EscapeHandler;
import io.seata.sqlparser.EscapeHandlerFactory;
import io.seata.sqlparser.util.JdbcConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
<<<<<<<< HEAD:rm-datasource/src/test/java/io/seata/rm/datasource/undo/polardbx/keyword/PolarDBXEscapeHandlerTest.java
 * The type PolarDB-X keyword checker test.
 *
 * @author hsien999
 */
public class PolarDBXEscapeHandlerTest {
    @Test
    public void testOracleKeywordChecker() {
        EscapeHandler escapeHandler = EscapeHandlerFactory.getEscapeHandler(JdbcConstants.POLARDBX);
        Assertions.assertNotNull(escapeHandler);
========
 * @author liuqiufeng
 */
public class ExtensionDefinitionTest {

    @Test
    public void testEquals() {
        ExtensionDefinition<ChineseHello> definition
                = new ExtensionDefinition<>("abc", 1, Scope.PROTOTYPE, ChineseHello.class);
        ExtensionDefinition<ChineseHello> definition2
                = new ExtensionDefinition<>("abc", 1, Scope.PROTOTYPE, ChineseHello.class);
        Assertions.assertEquals(definition2, definition);
>>>>>>>> upstream/2.x:common/src/test/java/io/seata/common/loader/ExtensionDefinitionTest.java
    }
}
