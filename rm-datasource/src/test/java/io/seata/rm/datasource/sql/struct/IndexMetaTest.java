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
package io.seata.rm.datasource.sql.struct;

import com.google.common.collect.Lists;
import io.seata.sqlparser.struct.ColumnMeta;
import io.seata.sqlparser.struct.IndexMeta;
import io.seata.sqlparser.struct.IndexType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author will
 */
public class IndexMetaTest {

    @Test
    public void testIndexMeta() {
        IndexMeta indexMeta = new IndexMeta();
        indexMeta.setValues(Lists.newArrayList());
        Assertions.assertNotNull(indexMeta.toString());

        IndexMeta other = new IndexMeta();
        other.setValues(Lists.newArrayList(new ColumnMeta()));
        Assertions.assertNotEquals(indexMeta, other);

        other = new IndexMeta();
        other.setNonUnique(true);
        Assertions.assertNotEquals(indexMeta, other);

        other = new IndexMeta();
        other.setIndexQualifier("");
        Assertions.assertNotEquals(indexMeta, other);

        other = new IndexMeta();
        other.setIndexName("");
        Assertions.assertNotEquals(indexMeta, other);

        other = new IndexMeta();
        other.setType((short)1);
        Assertions.assertNotEquals(indexMeta, other);

        other = new IndexMeta();
        indexMeta.setIndextype(IndexType.PRIMARY);
        other.setIndextype(IndexType.NORMAL);
        Assertions.assertNotEquals(indexMeta, other);

        other = new IndexMeta();
        other.setAscOrDesc("");
        //prevent npe and make the unit test go equals ascOrDesc
        other.setIndextype(IndexType.NORMAL);
        indexMeta.setIndextype(IndexType.NORMAL);
        Assertions.assertNotEquals(indexMeta, other);

        other = new IndexMeta();
        other.setOrdinalPosition(1);
        //prevent npe and make the unit test go equals ordinal position
        other.setIndextype(IndexType.NORMAL);
        indexMeta.setIndextype(IndexType.NORMAL);
        Assertions.assertNotEquals(indexMeta, other);

        other = new IndexMeta();
        other.setIndextype(IndexType.NORMAL);
        indexMeta.setIndextype(IndexType.NORMAL);
        Assertions.assertEquals(indexMeta, other);
    }

}
