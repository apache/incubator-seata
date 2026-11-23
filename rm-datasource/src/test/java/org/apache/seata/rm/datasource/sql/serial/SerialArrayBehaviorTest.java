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
package org.apache.seata.rm.datasource.sql.serial;

import org.junit.jupiter.api.Test;

import javax.sql.rowset.serial.SerialBlob;
import javax.sql.rowset.serial.SerialClob;
import javax.sql.rowset.serial.SerialDatalink;
import javax.sql.rowset.serial.SerialJavaObject;
import java.net.URL;
import java.sql.Array;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SerialArrayBehaviorTest {
    private SerialArray array(int type, Object value) throws Exception {
        Array source = mock(Array.class);
        when(source.getArray()).thenReturn(new Object[] {value});
        when(source.getBaseType()).thenReturn(type);
        when(source.getBaseTypeName()).thenReturn("TYPE");
        return new SerialArray(source);
    }

    @Test
    void materializesLobsLinksAndJavaObjects() throws Exception {
        SerialArray blob = array(Types.BLOB, new SerialBlob(new byte[] {1, 2}));
        assertArrayEquals(new byte[] {1, 2}, ((SerialBlob) blob.getElements()[0]).getBytes(1, 2));
        java.sql.Clob source = mock(java.sql.Clob.class);
        when(source.length()).thenReturn(4L);
        when(source.getCharacterStream()).thenReturn(new java.io.StringReader("text"));
        when(source.getAsciiStream()).thenReturn(new java.io.ByteArrayInputStream(new byte[] {116, 101, 120, 116}));
        SerialArray clob = array(Types.CLOB, source);
        assertEquals("text", ((SerialClob) clob.getElements()[0]).getSubString(1, 4));
        URL url = new URL("https://example.com/value");
        assertEquals(url, ((SerialDatalink) array(Types.DATALINK, url).getElements()[0]).getDatalink());
        assertEquals(
                "value", ((SerialJavaObject) array(Types.JAVA_OBJECT, "value").getElements()[0]).getObject());
    }

    @Test
    void comparesTypeAndElementsAndSupportsFree() throws Exception {
        SerialArray first = array(Types.INTEGER, 1);
        SerialArray same = array(Types.INTEGER, 1);
        assertEquals(first, first);
        assertEquals(first, same);
        assertEquals(first.hashCode(), same.hashCode());
        assertNotEquals(first, array(Types.INTEGER, 2));
        assertNotEquals(first, array(Types.VARCHAR, 1));
        same.setBaseTypeName("OTHER");
        assertNotEquals(first, same);
        assertNotEquals(first, null);
        assertNotEquals(first, "value");
        assertNull(first.getResultSet());
        assertNull(first.getResultSet(Collections.emptyMap()));
        assertNull(first.getResultSet(1, 1));
        assertNull(first.getResultSet(1, 1, Collections.emptyMap()));
        assertSame(first.getElements(), first.getArray(Collections.emptyMap()));
        first.free();
        first.free();
        assertNull(first.getArray());
        assertNull(first.getBaseTypeName());
        first.setElements(null);
        assertNull(first.getElements());
    }

    @Test
    void rejectsMissingDriverArray() throws Exception {
        assertThrows(SQLException.class, () -> new SerialArray(null));
        assertThrows(SQLException.class, () -> new SerialArray(mock(Array.class)));
    }
}
