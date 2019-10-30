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

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.JDBCType;
import java.sql.SQLException;
import java.sql.Timestamp;

import javax.sql.rowset.serial.SerialBlob;
import javax.sql.rowset.serial.SerialClob;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.seata.rm.datasource.DataCompareUtils;
import io.seata.rm.datasource.sql.struct.Field;
import io.seata.rm.datasource.undo.BaseUndoLogParserTest;
import io.seata.rm.datasource.undo.UndoLogParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Geng Zhang
 */
public class JacksonUndoLogParserTest extends BaseUndoLogParserTest {

    JacksonUndoLogParser parser = new JacksonUndoLogParser();

    @Test
    public void encode() throws NoSuchFieldException, IllegalAccessException, IOException, SQLException {
        //get the jackson mapper
        java.lang.reflect.Field reflectField = parser.getClass().getDeclaredField("MAPPER");
        reflectField.setAccessible(true);
        ObjectMapper mapper = (ObjectMapper)reflectField.get(null);

        //bigint type
        Field field = new Field("bigint_type", JDBCType.BIGINT.getVendorTypeNumber(), 9223372036854775807L);
        byte[] bytes = mapper.writeValueAsBytes(field);
        Field sameField = mapper.readValue(bytes, Field.class);
        Assertions.assertTrue(DataCompareUtils.isFieldEquals(field, sameField).getResult());

        //big decimal type
        field = new Field("decimal_type", JDBCType.DECIMAL.getVendorTypeNumber(), new BigDecimal("55555555555555555555.55555555555555555555"));
        bytes = mapper.writeValueAsBytes(field);
        sameField = mapper.readValue(bytes, Field.class);
        Assertions.assertTrue(DataCompareUtils.isFieldEquals(field, sameField).getResult());

        //double type
        field = new Field("double_type", JDBCType.DOUBLE.getVendorTypeNumber(), 999999.999999999);
        bytes = mapper.writeValueAsBytes(field);
        sameField = mapper.readValue(bytes, Field.class);
        Assertions.assertTrue(DataCompareUtils.isFieldEquals(field, sameField).getResult());

        //timestamp type
        field = new Field("timestamp_type", JDBCType.TIMESTAMP.getVendorTypeNumber(), Timestamp.valueOf("2019-08-10 10:49:26.926554"));
        bytes = mapper.writeValueAsBytes(field);
        sameField = mapper.readValue(bytes, Field.class);
        Assertions.assertTrue(DataCompareUtils.isFieldEquals(field, sameField).getResult());

        //blob type
        field = new Field("blob_type", JDBCType.BLOB.getVendorTypeNumber(), new SerialBlob("hello".getBytes()));
        bytes = mapper.writeValueAsBytes(field);
        sameField = mapper.readValue(bytes, Field.class);
        Assertions.assertTrue(DataCompareUtils.isFieldEquals(field, sameField).getResult());

        //clob type
        field = new Field("clob_type", JDBCType.CLOB.getVendorTypeNumber(), new SerialClob("hello".toCharArray()));
        bytes = mapper.writeValueAsBytes(field);
        sameField = mapper.readValue(bytes, Field.class);
        Assertions.assertTrue(DataCompareUtils.isFieldEquals(field, sameField).getResult());
    }

    @Override
    public UndoLogParser getParser() {
        return parser;
    }
}