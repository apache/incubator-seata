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

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.RowIdLifetime;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import com.alibaba.druid.mock.MockStatementBase;
import com.alibaba.druid.mock.handler.MockExecuteHandler;
import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.util.jdbc.ResultSetMetaDataBase;
import io.seata.rm.datasource.DataSourceProxy;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * The table meta fetch test.
 *
 * @author hanwen created at 2019-02-01
 */
public class TableMetaTest {

    private static Object[][] columnMetas =
        new Object[][] {
            new Object[] {"", "", "t1", "id", Types.INTEGER, "INTEGER", 64, 0, 10, 1, "", "", 0, 0, 64, 1, "NO", "YES"},
            new Object[] {"", "", "t1", "name1", Types.VARCHAR, "VARCHAR", 64, 0, 10, 0, "", "", 0, 0, 64, 2, "YES",
                "NO"},
            new Object[] {"", "", "t1", "name2", Types.VARCHAR, "VARCHAR", 64, 0, 10, 0, "", "", 0, 0, 64, 3, "YES",
                "NO"},
            new Object[] {"", "", "t1", "name3", Types.VARCHAR, "VARCHAR", 64, 0, 10, 0, "", "", 0, 0, 64, 4, "YES",
                "NO"}
        };

    private static Object[][] indexMetas =
        new Object[][] {
            new Object[] {"PRIMARY", "id", false, "", 3, 1, "A", 34},
            new Object[] {"name1", "name1", false, "", 3, 1, "A", 34}
        };

    /**
     * The table meta fetch test.
     */
    @Test
    public void getTableMetaTest_0() {

        MockDriver mockDriver = new MockDriver();
        mockDriver.setExecuteHandler(new MockExecuteHandler() {
            @Override
            public ResultSet executeQuery(MockStatementBase statement, String s) throws SQLException {

                com.alibaba.druid.mock.MockResultSet resultSet = new com.alibaba.druid.mock.MockResultSet(statement);

                // just fetch meta from select * from `table` limit 1
                List<ResultSetMetaDataBase.ColumnMetaData> columns = resultSet.getMockMetaData().getColumns();
                columns.add(new ResultSetMetaDataBase.ColumnMetaData());
                columns.add(new ResultSetMetaDataBase.ColumnMetaData());
                columns.add(new ResultSetMetaDataBase.ColumnMetaData());
                columns.add(new ResultSetMetaDataBase.ColumnMetaData());

                return resultSet;
            }
        });
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:mock:xxx");
        dataSource.setDriver(mockDriver);

        DataSourceProxy proxy = new DataSourceProxy(dataSource);

        TableMeta tableMeta = TableMetaCache.getTableMeta(proxy, "t1");

        Assertions.assertEquals("t1", tableMeta.getTableName());
        Assertions.assertEquals("id", tableMeta.getPkName());

        Assertions.assertEquals("id", tableMeta.getColumnMeta("id").getColumnName());
        Assertions.assertEquals("id", tableMeta.getAutoIncreaseColumn().getColumnName());
        Assertions.assertEquals(1, tableMeta.getPrimaryKeyMap().size());
        Assertions.assertEquals(Collections.singletonList("id"), tableMeta.getPrimaryKeyOnlyName());

        Assertions.assertEquals(4, tableMeta.getAllColumns().size());

        assertColumnMetaEquals(columnMetas[0], tableMeta.getAllColumns().get("id"));
        assertColumnMetaEquals(columnMetas[1], tableMeta.getAllColumns().get("name1"));
        assertColumnMetaEquals(columnMetas[2], tableMeta.getAllColumns().get("name2"));
        assertColumnMetaEquals(columnMetas[3], tableMeta.getAllColumns().get("name3"));

        Assertions.assertEquals(2, tableMeta.getAllIndexes().size());

        assertIndexMetaEquals(indexMetas[0], tableMeta.getAllIndexes().get("PRIMARY"));
        Assertions.assertEquals(IndexType.PRIMARY, tableMeta.getAllIndexes().get("PRIMARY").getIndextype());
        assertIndexMetaEquals(indexMetas[1], tableMeta.getAllIndexes().get("name1"));
        Assertions.assertEquals(IndexType.Unique, tableMeta.getAllIndexes().get("name1").getIndextype());

    }

    private void assertColumnMetaEquals(Object[] expected, ColumnMeta actual) {
        Assertions.assertEquals(expected[0], actual.getTableCat());
        Assertions.assertEquals(expected[3], actual.getColumnName());
        Assertions.assertEquals(expected[4], actual.getDataType());
        Assertions.assertEquals(expected[5], actual.getDataTypeName());
        Assertions.assertEquals(expected[6], actual.getColumnSize());
        Assertions.assertEquals(expected[7], actual.getDecimalDigits());
        Assertions.assertEquals(expected[8], actual.getNumPrecRadix());
        Assertions.assertEquals(expected[9], actual.getNullAble());
        Assertions.assertEquals(expected[10], actual.getRemarks());
        Assertions.assertEquals(expected[11], actual.getColumnDef());
        Assertions.assertEquals(expected[12], actual.getSqlDataType());
        Assertions.assertEquals(expected[13], actual.getSqlDatetimeSub());
        Assertions.assertEquals(expected[14], actual.getCharOctetLength());
        Assertions.assertEquals(expected[15], actual.getOrdinalPosition());
        Assertions.assertEquals(expected[16], actual.getIsNullAble());
        Assertions.assertEquals(expected[17], actual.getIsAutoincrement());
    }

    private void assertIndexMetaEquals(Object[] expected, IndexMeta actual) {
        Assertions.assertEquals(expected[0], actual.getIndexName());
        Assertions.assertEquals(expected[3], actual.getIndexQualifier());
        Assertions.assertEquals(expected[4], (int)actual.getType());
        Assertions.assertEquals(expected[5], actual.getOrdinalPosition());
        Assertions.assertEquals(expected[6], actual.getAscOrDesc());
        Assertions.assertEquals(expected[7], actual.getCardinality());
    }

    private class MockDriver extends com.alibaba.druid.mock.MockDriver {

        @Override
        public MockConnection createMockConnection(com.alibaba.druid.mock.MockDriver driver, String url,
                                                   Properties connectProperties) {
            return new MockConnection(this, url, connectProperties);
        }
    }

    private class MockResultSet extends com.alibaba.druid.mock.MockResultSet {

        private List<String> columnLabels;

        /**
         * Instantiates a new Mock result set.
         *
         * @param statement    the statement
         * @param columnLabels the column labels
         */
        public MockResultSet(Statement statement, List<String> columnLabels) {
            super(statement);
            this.columnLabels = new ArrayList<>(columnLabels);
        }

        @Override
        public int findColumn(String columnLabel) throws SQLException {
            return columnLabels.indexOf(columnLabel) + 1;
        }
    }

    private class MockConnection extends com.alibaba.druid.mock.MockConnection {

        /**
         * Instantiates a new Mock connection.
         *
         * @param driver            the driver
         * @param url               the url
         * @param connectProperties the connect properties
         */
        public MockConnection(com.alibaba.druid.mock.MockDriver driver, String url, Properties connectProperties) {
            super(driver, url, connectProperties);
        }

        @Override
        public DatabaseMetaData getMetaData() throws SQLException {

            return new DatabaseMetaData() {

                @Override
                public ResultSet getColumns(String catalog, String schemaPattern, String tableNamePattern,
                                            String columnNamePattern) throws SQLException {
                    List<String> columnLabels = Arrays.asList(
                        "TABLE_CAT",
                        "TABLE_SCHEM",
                        "TABLE_NAME",
                        "COLUMN_NAME",
                        "DATA_TYPE",
                        "TYPE_NAME",
                        "COLUMN_SIZE",
                        "DECIMAL_DIGITS",
                        "NUM_PREC_RADIX",
                        "NULLABLE",
                        "REMARKS",
                        "COLUMN_DEF",
                        "SQL_DATA_TYPE",
                        "SQL_DATETIME_SUB",
                        "CHAR_OCTET_LENGTH",
                        "ORDINAL_POSITION",
                        "IS_NULLABLE",
                        "IS_AUTOINCREMENT"
                    );

                    MockResultSet resultSet = new MockResultSet(createStatement(), columnLabels);

                    List<ResultSetMetaDataBase.ColumnMetaData> columns = resultSet.getMockMetaData().getColumns();
                    for (String columnLabel : columnLabels) {
                        ResultSetMetaDataBase.ColumnMetaData column = new ResultSetMetaDataBase.ColumnMetaData();
                        column.setColumnName(columnLabel);
                        columns.add(column);
                    }

                    resultSet.getRows().add(columnMetas[0]);
                    resultSet.getRows().add(columnMetas[1]);
                    resultSet.getRows().add(columnMetas[2]);
                    resultSet.getRows().add(columnMetas[3]);

                    return resultSet;
                }

                @Override
                public ResultSet getIndexInfo(String catalog, String schema, String table, boolean unique,
                                              boolean approximate) throws SQLException {

                    List<String> columnLabels = Arrays.asList(
                        "INDEX_NAME",
                        "COLUMN_NAME",
                        "NON_UNIQUE",
                        "INDEX_QUALIFIER",
                        "TYPE",
                        "ORDINAL_POSITION",
                        "ASC_OR_DESC",
                        "CARDINALITY"
                    );

                    MockResultSet resultSet = new MockResultSet(createStatement(), columnLabels);

                    List<ResultSetMetaDataBase.ColumnMetaData> columns = resultSet.getMockMetaData().getColumns();
                    for (String columnLabel : columnLabels) {
                        ResultSetMetaDataBase.ColumnMetaData column = new ResultSetMetaDataBase.ColumnMetaData();
                        column.setColumnName(columnLabel);
                        columns.add(column);
                    }

                    resultSet.getRows().add(indexMetas[0]);
                    resultSet.getRows().add(indexMetas[1]);

                    return resultSet;
                }

                @Override
                public boolean allProceduresAreCallable() throws SQLException {
                    return false;
                }

                @Override
                public boolean allTablesAreSelectable() throws SQLException {
                    return false;
                }

                @Override
                public String getURL() throws SQLException {
                    return getUrl();
                }

                @Override
                public String getUserName() throws SQLException {
                    return null;
                }

                @Override
                public boolean isReadOnly() throws SQLException {
                    return false;
                }

                @Override
                public boolean nullsAreSortedHigh() throws SQLException {
                    return false;
                }

                @Override
                public boolean nullsAreSortedLow() throws SQLException {
                    return false;
                }

                @Override
                public boolean nullsAreSortedAtStart() throws SQLException {
                    return false;
                }

                @Override
                public boolean nullsAreSortedAtEnd() throws SQLException {
                    return false;
                }

                @Override
                public String getDatabaseProductName() throws SQLException {
                    return null;
                }

                @Override
                public String getDatabaseProductVersion() throws SQLException {
                    return null;
                }

                @Override
                public String getDriverName() throws SQLException {
                    return null;
                }

                @Override
                public String getDriverVersion() throws SQLException {
                    return null;
                }

                @Override
                public int getDriverMajorVersion() {
                    return 0;
                }

                @Override
                public int getDriverMinorVersion() {
                    return 0;
                }

                @Override
                public boolean usesLocalFiles() throws SQLException {
                    return false;
                }

                @Override
                public boolean usesLocalFilePerTable() throws SQLException {
                    return false;
                }

                @Override
                public boolean supportsMixedCaseIdentifiers() throws SQLException {
                    return false;
                }

                @Override
                public boolean storesUpperCaseIdentifiers() throws SQLException {
                    return false;
                }

                @Override
                public boolean storesLowerCaseIdentifiers() throws SQLException {
                    return false;
                }

                @Override
                public boolean storesMixedCaseIdentifiers() throws SQLException {
                    return false;
                }

                @Override
                public boolean supportsMixedCaseQuotedIdentifiers() throws SQLException {
                    return false;
                }

                @Override
                public boolean storesUpperCaseQuotedIdentifiers() throws SQLException {
                    return false;
                }

                @Override
                public boolean storesLowerCaseQuotedIdentifiers() throws SQLException {
                    return false;
                }

                @Override
                public boolean storesMixedCaseQuotedIdentifiers() throws SQLException {
                    return false;
                }

                @Override
                public String getIdentifierQuoteString() throws SQLException {
                    return null;
                }

                @Override
                public String getSQLKeywords() throws SQLException {
                    return null;
                }

                @Override
                public String getNumericFunctions() throws SQLException {
                    return null;
                }

                @Override
                public String getStringFunctions() throws SQLException {
                    return null;
                }

                @Override
                public String getSystemFunctions() throws SQLException {
                    return null;
                }

                @Override
                public String getTimeDateFunctions() throws SQLException {
                    return null;
                }

                @Override
                public String getSearchStringEscape() throws SQLException {
                    return null;
                }

                @Override
                public String getExtraNameCharacters() throws SQLException {
                    return null;
                }

                @Override
                public boolean supportsAlterTableWithAddColumn() throws SQLException {
                    return false;
                }

                @Override
                public boolean supportsAlterTableWithDropColumn() throws SQLException {
                    return false;
                }

                @Override
                public boolean supportsColumnAliasing() throws SQLException {
                    return false;
                }

                @Override
                public boolean nullPlusNonNullIsNull() throws SQLException {
                    return false;
                }

                @Override
                public boolean supportsConvert() throws SQLException {
                    return false;
                }

                @Override
                public boolean supportsConvert(int fromType, int toType) throws SQLException {
                    return false;
                }

                @Override
                public boolean supportsTableCorrelationNames() throws SQLException {
                    return false;
                }

                @Override
                public boolean supportsDifferentTableCorrelationNames() throws SQLException {
                    return false;
                }

                @Override
                public boolean supportsExpressionsInOrderBy() throws SQLException {
                    return false;
                }

                @Override
                public boolean supportsOrderByUnrelated() throws SQLException {
                    return false;
                }

                @Override
                public boolean supportsGroupBy() throws SQLException {
                    return false;
                }

                @Override
                public boolean supportsGroupByUnrelated() throws SQLException {
                    return false;
                }

                @Override
                public boolean supportsGroupByBeyondSelect() throws SQLException {
                    return false;
                }

                @Override
                public boolean supportsLikeEscapeClause() throws SQLException {
                    return false;
                }

                @Override
                public boolean supportsMultipleResultSets() throws SQLException {
                    return false;
                }

                @Override
                public boolean supportsMultipleTransactions() throws SQLException {
                    return false;
                }

                @Override
                public boolean supportsNonNullableColumns() throws SQLException {
                    return false;
                }

                @Override
                public boolean supportsMinimumSQLGrammar() throws SQLException {
                    return false;
                }

                @Override
                public boolean supportsCoreSQLGrammar() throws SQLException {
                    return false;
                }

                @Override
                public boolean supportsExtendedSQLGrammar() throws SQLException {
                    return false;
                }

                @Override
                public boolean supportsANSI92EntryLevelSQL() throws SQLException {
                    return false;
                }

                @Override
                public boolean supportsANSI92IntermediateSQL() throws SQLException {
                    return false;
                }

                @Override
                public boolean supportsANSI92FullSQL() throws SQLException {
                    return false;
                }

                @Override
                public boolean supportsIntegrityEnhancementFacility() throws SQLException {
                    return false;
                }

                @Override
                public boolean supportsOuterJoins() throws SQLException {
                    return false;
                }

                @Override
                public boolean supportsFullOuterJoins() throws SQLException {
                    return false;
                }

                @Override
                public boolean supportsLimitedOuterJoins() throws SQLException {
                    return false;
                }

                @Override
                public String getSchemaTerm() throws SQLException {
                    return null;
                }

                @Override
                public String getProcedureTerm() throws SQLException {
                    return null;
                }

                @Override
                public String getCatalogTerm() throws SQLException {
                    return null;
                }

                @Override
                public boolean isCatalogAtStart() throws SQLException {
                    return false;
                }

                @Override
                public String getCatalogSeparator() throws SQLException {
                    return null;
                }

                @Override
                public boolean supportsSchemasInDataManipulation() throws SQLException {
                    return false;
                }

                @Override
                public boolean supportsSchemasInProcedureCalls() throws SQLException {
                    return false;
                }

                @Override
                public boolean supportsSchemasInTableDefinitions() throws SQLException {
                    return false;
                }

                @Override
                public boolean supportsSchemasInIndexDefinitions() throws SQLException {
                    return false;
                }

                @Override
                public boolean supportsSchemasInPrivilegeDefinitions() throws SQLException {
                    return false;
                }

                @Override
                public boolean supportsCatalogsInDataManipulation() throws SQLException {
                    return false;
                }

                @Override
                public boolean supportsCatalogsInProcedureCalls() throws SQLException {
                    return false;
                }

                @Override
                public boolean supportsCatalogsInTableDefinitions() throws SQLException {
                    return false;
                }

                @Override
                public boolean supportsCatalogsInIndexDefinitions() throws SQLException {
                    return false;
                }

                @Override
                public boolean supportsCatalogsInPrivilegeDefinitions() throws SQLException {
                    return false;
                }

                @Override
                public boolean supportsPositionedDelete() throws SQLException {
                    return false;
                }

                @Override
                public boolean supportsPositionedUpdate() throws SQLException {
                    return false;
                }

                @Override
                public boolean supportsSelectForUpdate() throws SQLException {
                    return false;
                }

                @Override
                public boolean supportsStoredProcedures() throws SQLException {
                    return false;
                }

                @Override
                public boolean supportsSubqueriesInComparisons() throws SQLException {
                    return false;
                }

                @Override
                public boolean supportsSubqueriesInExists() throws SQLException {
                    return false;
                }

                @Override
                public boolean supportsSubqueriesInIns() throws SQLException {
                    return false;
                }

                @Override
                public boolean supportsSubqueriesInQuantifieds() throws SQLException {
                    return false;
                }

                @Override
                public boolean supportsCorrelatedSubqueries() throws SQLException {
                    return false;
                }

                @Override
                public boolean supportsUnion() throws SQLException {
                    return false;
                }

                @Override
                public boolean supportsUnionAll() throws SQLException {
                    return false;
                }

                @Override
                public boolean supportsOpenCursorsAcrossCommit() throws SQLException {
                    return false;
                }

                @Override
                public boolean supportsOpenCursorsAcrossRollback() throws SQLException {
                    return false;
                }

                @Override
                public boolean supportsOpenStatementsAcrossCommit() throws SQLException {
                    return false;
                }

                @Override
                public boolean supportsOpenStatementsAcrossRollback() throws SQLException {
                    return false;
                }

                @Override
                public int getMaxBinaryLiteralLength() throws SQLException {
                    return 0;
                }

                @Override
                public int getMaxCharLiteralLength() throws SQLException {
                    return 0;
                }

                @Override
                public int getMaxColumnNameLength() throws SQLException {
                    return 0;
                }

                @Override
                public int getMaxColumnsInGroupBy() throws SQLException {
                    return 0;
                }

                @Override
                public int getMaxColumnsInIndex() throws SQLException {
                    return 0;
                }

                @Override
                public int getMaxColumnsInOrderBy() throws SQLException {
                    return 0;
                }

                @Override
                public int getMaxColumnsInSelect() throws SQLException {
                    return 0;
                }

                @Override
                public int getMaxColumnsInTable() throws SQLException {
                    return 0;
                }

                @Override
                public int getMaxConnections() throws SQLException {
                    return 0;
                }

                @Override
                public int getMaxCursorNameLength() throws SQLException {
                    return 0;
                }

                @Override
                public int getMaxIndexLength() throws SQLException {
                    return 0;
                }

                @Override
                public int getMaxSchemaNameLength() throws SQLException {
                    return 0;
                }

                @Override
                public int getMaxProcedureNameLength() throws SQLException {
                    return 0;
                }

                @Override
                public int getMaxCatalogNameLength() throws SQLException {
                    return 0;
                }

                @Override
                public int getMaxRowSize() throws SQLException {
                    return 0;
                }

                @Override
                public boolean doesMaxRowSizeIncludeBlobs() throws SQLException {
                    return false;
                }

                @Override
                public int getMaxStatementLength() throws SQLException {
                    return 0;
                }

                @Override
                public int getMaxStatements() throws SQLException {
                    return 0;
                }

                @Override
                public int getMaxTableNameLength() throws SQLException {
                    return 0;
                }

                @Override
                public int getMaxTablesInSelect() throws SQLException {
                    return 0;
                }

                @Override
                public int getMaxUserNameLength() throws SQLException {
                    return 0;
                }

                @Override
                public int getDefaultTransactionIsolation() throws SQLException {
                    return 0;
                }

                @Override
                public boolean supportsTransactions() throws SQLException {
                    return false;
                }

                @Override
                public boolean supportsTransactionIsolationLevel(int level) throws SQLException {
                    return false;
                }

                @Override
                public boolean supportsDataDefinitionAndDataManipulationTransactions() throws SQLException {
                    return false;
                }

                @Override
                public boolean supportsDataManipulationTransactionsOnly() throws SQLException {
                    return false;
                }

                @Override
                public boolean dataDefinitionCausesTransactionCommit() throws SQLException {
                    return false;
                }

                @Override
                public boolean dataDefinitionIgnoredInTransactions() throws SQLException {
                    return false;
                }

                @Override
                public ResultSet getProcedures(String catalog, String schemaPattern, String procedureNamePattern)
                    throws SQLException {
                    return null;
                }

                @Override
                public ResultSet getProcedureColumns(String catalog, String schemaPattern, String procedureNamePattern,
                                                     String columnNamePattern) throws SQLException {
                    return null;
                }

                @Override
                public ResultSet getTables(String catalog, String schemaPattern, String tableNamePattern,
                                           String[] types) throws SQLException {
                    return null;
                }

                @Override
                public ResultSet getSchemas() throws SQLException {
                    return null;
                }

                @Override
                public ResultSet getCatalogs() throws SQLException {
                    return null;
                }

                @Override
                public ResultSet getTableTypes() throws SQLException {
                    return null;
                }

                @Override
                public ResultSet getColumnPrivileges(String catalog, String schema, String table,
                                                     String columnNamePattern) throws SQLException {
                    return null;
                }

                @Override
                public ResultSet getTablePrivileges(String catalog, String schemaPattern, String tableNamePattern)
                    throws SQLException {
                    return null;
                }

                @Override
                public ResultSet getBestRowIdentifier(String catalog, String schema, String table, int scope,
                                                      boolean nullable) throws SQLException {
                    return null;
                }

                @Override
                public ResultSet getVersionColumns(String catalog, String schema, String table) throws SQLException {
                    return null;
                }

                @Override
                public ResultSet getPrimaryKeys(String catalog, String schema, String table) throws SQLException {
                    return null;
                }

                @Override
                public ResultSet getImportedKeys(String catalog, String schema, String table) throws SQLException {
                    return null;
                }

                @Override
                public ResultSet getExportedKeys(String catalog, String schema, String table) throws SQLException {
                    return null;
                }

                @Override
                public ResultSet getCrossReference(String parentCatalog, String parentSchema, String parentTable,
                                                   String foreignCatalog, String foreignSchema, String foreignTable)
                    throws SQLException {
                    return null;
                }

                @Override
                public ResultSet getTypeInfo() throws SQLException {
                    return null;
                }

                @Override
                public boolean supportsResultSetType(int type) throws SQLException {
                    return false;
                }

                @Override
                public boolean supportsResultSetConcurrency(int type, int concurrency) throws SQLException {
                    return false;
                }

                @Override
                public boolean ownUpdatesAreVisible(int type) throws SQLException {
                    return false;
                }

                @Override
                public boolean ownDeletesAreVisible(int type) throws SQLException {
                    return false;
                }

                @Override
                public boolean ownInsertsAreVisible(int type) throws SQLException {
                    return false;
                }

                @Override
                public boolean othersUpdatesAreVisible(int type) throws SQLException {
                    return false;
                }

                @Override
                public boolean othersDeletesAreVisible(int type) throws SQLException {
                    return false;
                }

                @Override
                public boolean othersInsertsAreVisible(int type) throws SQLException {
                    return false;
                }

                @Override
                public boolean updatesAreDetected(int type) throws SQLException {
                    return false;
                }

                @Override
                public boolean deletesAreDetected(int type) throws SQLException {
                    return false;
                }

                @Override
                public boolean insertsAreDetected(int type) throws SQLException {
                    return false;
                }

                @Override
                public boolean supportsBatchUpdates() throws SQLException {
                    return false;
                }

                @Override
                public ResultSet getUDTs(String catalog, String schemaPattern, String typeNamePattern, int[] types)
                    throws SQLException {
                    return null;
                }

                @Override
                public Connection getConnection() throws SQLException {
                    return null;
                }

                @Override
                public boolean supportsSavepoints() throws SQLException {
                    return false;
                }

                @Override
                public boolean supportsNamedParameters() throws SQLException {
                    return false;
                }

                @Override
                public boolean supportsMultipleOpenResults() throws SQLException {
                    return false;
                }

                @Override
                public boolean supportsGetGeneratedKeys() throws SQLException {
                    return false;
                }

                @Override
                public ResultSet getSuperTypes(String catalog, String schemaPattern, String typeNamePattern)
                    throws SQLException {
                    return null;
                }

                @Override
                public ResultSet getSuperTables(String catalog, String schemaPattern, String tableNamePattern)
                    throws SQLException {
                    return null;
                }

                @Override
                public ResultSet getAttributes(String catalog, String schemaPattern, String typeNamePattern,
                                               String attributeNamePattern) throws SQLException {
                    return null;
                }

                @Override
                public boolean supportsResultSetHoldability(int holdability) throws SQLException {
                    return false;
                }

                @Override
                public int getResultSetHoldability() throws SQLException {
                    return 0;
                }

                @Override
                public int getDatabaseMajorVersion() throws SQLException {
                    return 0;
                }

                @Override
                public int getDatabaseMinorVersion() throws SQLException {
                    return 0;
                }

                @Override
                public int getJDBCMajorVersion() throws SQLException {
                    return 0;
                }

                @Override
                public int getJDBCMinorVersion() throws SQLException {
                    return 0;
                }

                @Override
                public int getSQLStateType() throws SQLException {
                    return 0;
                }

                @Override
                public boolean locatorsUpdateCopy() throws SQLException {
                    return false;
                }

                @Override
                public boolean supportsStatementPooling() throws SQLException {
                    return false;
                }

                @Override
                public RowIdLifetime getRowIdLifetime() throws SQLException {
                    return null;
                }

                @Override
                public ResultSet getSchemas(String catalog, String schemaPattern) throws SQLException {
                    return null;
                }

                @Override
                public boolean supportsStoredFunctionsUsingCallSyntax() throws SQLException {
                    return false;
                }

                @Override
                public boolean autoCommitFailureClosesAllResultSets() throws SQLException {
                    return false;
                }

                @Override
                public ResultSet getClientInfoProperties() throws SQLException {
                    return null;
                }

                @Override
                public ResultSet getFunctions(String catalog, String schemaPattern, String functionNamePattern)
                    throws SQLException {
                    return null;
                }

                @Override
                public ResultSet getFunctionColumns(String catalog, String schemaPattern, String functionNamePattern,
                                                    String columnNamePattern) throws SQLException {
                    return null;
                }

                @Override
                public ResultSet getPseudoColumns(String catalog, String schemaPattern, String tableNamePattern,
                                                  String columnNamePattern) throws SQLException {
                    return null;
                }

                @Override
                public boolean generatedKeyAlwaysReturned() throws SQLException {
                    return false;
                }

                @Override
                public <T> T unwrap(Class<T> iface) throws SQLException {
                    return null;
                }

                @Override
                public boolean isWrapperFor(Class<?> iface) throws SQLException {
                    return false;
                }
            };
        }
    }
}
