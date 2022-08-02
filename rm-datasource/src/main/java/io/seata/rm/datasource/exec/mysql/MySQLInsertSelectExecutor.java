package io.seata.rm.datasource.exec.mysql;

import io.seata.common.util.CollectionUtils;
import io.seata.rm.datasource.ColumnUtils;
import io.seata.rm.datasource.StatementProxy;
import io.seata.rm.datasource.exec.StatementCallback;
import io.seata.rm.datasource.exec.constant.SQLTypeConstant;
import io.seata.rm.datasource.exec.handler.AfterHandler;
import io.seata.rm.datasource.exec.handler.AfterHandlerFactory;
import io.seata.rm.datasource.exec.handler.after.InsertOrUpdateHandler;
import io.seata.rm.datasource.sql.struct.Row;
import io.seata.rm.datasource.sql.struct.TableMeta;
import io.seata.rm.datasource.sql.struct.TableRecords;
import io.seata.sqlparser.SQLInsertRecognizer;
import io.seata.sqlparser.SQLRecognizer;
import io.seata.sqlparser.SQLType;
import io.seata.sqlparser.struct.Defaultable;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author: lyx
 */
public class MySQLInsertSelectExecutor extends MySQLInsertExecutor implements Defaultable {

    /**
     * insert recognizer from sql
     */
    private SQLInsertRecognizer insertRecognizer;

    private String selectSQL;

    private AfterHandler afterHandler;

    public String getSelectSQL() {
        return selectSQL;
    }

    public void setSelectSQL(String selectSQL) {
        this.selectSQL = selectSQL;
    }

    public MySQLInsertSelectExecutor(StatementProxy statementProxy, StatementCallback statementCallback, SQLRecognizer sqlRecognizer) throws SQLException {
        super(statementProxy, statementCallback, sqlRecognizer);
        createInsertRecognizer();
    }

    public void createInsertRecognizer() throws SQLException {
        SQLInsertRecognizer recognizer = (SQLInsertRecognizer) sqlRecognizer;
        // get the sql after insert
        String querySQL = recognizer.getQuerySQL();
        insertRecognizer = doCreateInsertRecognizer(querySQL);
        afterHandler = prepareAfterHandler();
    }

    private AfterHandler prepareAfterHandler() {
        SQLInsertRecognizer recognizer = (SQLInsertRecognizer) sqlRecognizer;
        return CollectionUtils.isNotEmpty(recognizer.getDuplicateKeyUpdate()) ?
                AfterHandlerFactory.getAfterHandler(SQLTypeConstant.INSERT_ON_DUPLICATE_UPDATE)
                : recognizer.isIgnore() ? AfterHandlerFactory.getAfterHandler(SQLTypeConstant.INSERT_IGNORE) : null;
    }

    @Override
    public TableRecords beforeImage() throws SQLException {
        TableMeta tableMeta = getTableMeta();
        if (Objects.isNull(afterHandler)) {
            return TableRecords.empty(tableMeta);
        }
        // after image sql the same of before image
        if (io.seata.common.util.StringUtils.isBlank(selectSQL)) {
            selectSQL = buildImageSQL(tableMeta);
        }
        if (CollectionUtils.isEmpty(paramAppenderMap)) {
            // insert on duplicate update can not get PK from drive
            if (afterHandler instanceof InsertOrUpdateHandler) {
                throw new IllegalArgumentException("can not find unique param,may be you should add unique key when use the sqlType of" +
                        " on duplicate key update ");
            }
            return TableRecords.empty(tableMeta);
        }
        return buildTableRecords2(tableMeta, selectSQL, new ArrayList<List<Object>>(paramAppenderMap.values()));
    }

    @Override
    protected TableRecords afterImage(TableRecords beforeImage) throws SQLException {
        TableMeta tableMeta = getTableMeta();
        if (Objects.nonNull(afterHandler) && CollectionUtils.isNotEmpty(paramAppenderMap)) {
            String afterSelectSQL = afterHandler.buildAfterSelectSQL(beforeImage);
            return buildTableRecords2(tableMeta, selectSQL + afterSelectSQL, new ArrayList<List<Object>>(paramAppenderMap.values()));
        }
        return super.afterImage(beforeImage);
    }

    @Override
    protected Map<SQLType, List<Row>> buildUndoRow(TableRecords beforeImage, TableRecords afterImage) {
        return afterHandler.buildUndoRow(beforeImage, afterImage);
    }

    /**
     * from the sql type to get insert rows
     * unless select insert,other in {@link SQLInsertRecognizer#getInsertRows(Collection)}
     *
     * @param primaryKeyIndex the primary key index
     * @return the insert rows
     */
    @Override
    public List<List<Object>> getInsertRows(Collection primaryKeyIndex) {
        return Objects.nonNull(insertRecognizer) ? insertRecognizer.getInsertRows(primaryKeyIndex) : Collections.emptyList();
    }

    /**
     * from the sql type to get insert params values
     * unless select insert,other in {@link SQLInsertRecognizer#getInsertParamsValue}
     *
     * @return the insert params values
     */
    @Override
    public List<String> getInsertParamsValue() {
        return Objects.nonNull(insertRecognizer) ? insertRecognizer.getInsertParamsValue() : Collections.emptyList();
    }

    @Override
    protected Map<String, ArrayList<Object>> buildImageParamperters(SQLInsertRecognizer recognizer) {
        List<String> insertParamsList = getInsertParamsValue();
        List<String> insertColumns = Optional.ofNullable(recognizer.getInsertColumns()).map(list -> list.stream()
                .map(column -> ColumnUtils.delEscape(column, getDbType())).collect(Collectors.toList())).orElse(null);
        if (CollectionUtils.isEmpty(insertColumns)) {
            insertColumns = getTableMeta(recognizer.getTableName()).getDefaultTableColumn();
        }
        Map<String, ArrayList<Object>> imageParamperterMap = new HashMap<>(insertColumns.size(), 1);

        for (String insertParams : insertParamsList) {
            String[] insertParamsArray = insertParams.split(",");
            for (int i = 0; i < insertColumns.size(); i++) {
                String m = ColumnUtils.delEscape(insertColumns.get(i), getDbType());
                String params = insertParamsArray[i];
                ArrayList<Object> imageListTemp = imageParamperterMap.computeIfAbsent(m, k -> new ArrayList<>());
                imageListTemp.add(params.trim());
                imageParamperterMap.put(m, imageListTemp);
            }
        }
        return imageParamperterMap;
    }
}
