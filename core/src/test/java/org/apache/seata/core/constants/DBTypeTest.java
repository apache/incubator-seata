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
package org.apache.seata.core.constants;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link DBType}.
 */
public class DBTypeTest {

    @Test
    void testValueofWithValidDbType() {
        assertEquals(DBType.MYSQL, DBType.valueof("mysql"));
        assertEquals(DBType.MYSQL, DBType.valueof("MYSQL"));
        assertEquals(DBType.MYSQL, DBType.valueof("MySQL"));
    }

    @Test
    void testValueofWithOracle() {
        assertEquals(DBType.ORACLE, DBType.valueof("oracle"));
        assertEquals(DBType.ORACLE, DBType.valueof("ORACLE"));
    }

    @Test
    void testValueofWithPostgresql() {
        assertEquals(DBType.POSTGRESQL, DBType.valueof("postgresql"));
        assertEquals(DBType.POSTGRESQL, DBType.valueof("POSTGRESQL"));
    }

    @Test
    void testValueofWithH2() {
        assertEquals(DBType.H2, DBType.valueof("h2"));
        assertEquals(DBType.H2, DBType.valueof("H2"));
    }

    @Test
    void testValueofWithSqlserver() {
        assertEquals(DBType.SQLSERVER, DBType.valueof("sqlserver"));
        assertEquals(DBType.SQLSERVER, DBType.valueof("SQLSERVER"));
    }

    @Test
    void testValueofWithDb2() {
        assertEquals(DBType.DB2, DBType.valueof("db2"));
        assertEquals(DBType.DB2, DBType.valueof("DB2"));
    }

    @Test
    void testValueofWithMariadb() {
        assertEquals(DBType.MARIADB, DBType.valueof("mariadb"));
        assertEquals(DBType.MARIADB, DBType.valueof("MARIADB"));
    }

    @Test
    void testValueofWithOceanbase() {
        assertEquals(DBType.OCEANBASE, DBType.valueof("oceanbase"));
        assertEquals(DBType.OCEANBASE, DBType.valueof("OCEANBASE"));
    }

    @Test
    void testValueofWithDm() {
        assertEquals(DBType.DM, DBType.valueof("dm"));
        assertEquals(DBType.DM, DBType.valueof("DM"));
    }

    @Test
    void testValueofWithPolardb() {
        assertEquals(DBType.POLARDB, DBType.valueof("polardb"));
        assertEquals(DBType.POLARDB, DBType.valueof("POLARDB"));
    }

    @Test
    void testValueofWithClickhouse() {
        assertEquals(DBType.CLICKHOUSE, DBType.valueof("clickhouse"));
        assertEquals(DBType.CLICKHOUSE, DBType.valueof("CLICKHOUSE"));
    }

    @Test
    void testValueofWithInvalidDbType() {
        assertThrows(IllegalArgumentException.class, () -> DBType.valueof("unknown"));
        assertThrows(IllegalArgumentException.class, () -> DBType.valueof(""));
        assertThrows(IllegalArgumentException.class, () -> DBType.valueof("invalid_db"));
    }

    @Test
    void testValueofWithNullDbType() {
        assertThrows(IllegalArgumentException.class, () -> DBType.valueof(null));
    }

    @Test
    void testAllDbTypesCanBeRetrieved() {
        for (DBType dbType : DBType.values()) {
            assertEquals(dbType, DBType.valueof(dbType.name()));
            assertEquals(dbType, DBType.valueof(dbType.name().toLowerCase()));
        }
    }

    @Test
    void testDbTypeEnumValues() {
        DBType[] values = DBType.values();
        assertTrue(values.length > 0);

        // Verify some common database types exist
        assertNotNull(DBType.valueOf("MYSQL"));
        assertNotNull(DBType.valueOf("ORACLE"));
        assertNotNull(DBType.valueOf("POSTGRESQL"));
        assertNotNull(DBType.valueOf("H2"));
        assertNotNull(DBType.valueOf("SQLSERVER"));
    }
}
