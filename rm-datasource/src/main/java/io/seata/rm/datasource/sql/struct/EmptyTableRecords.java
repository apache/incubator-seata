package io.seata.rm.datasource.sql.struct;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EmptyTableRecords extends TableRecords {

    public EmptyTableRecords() {
    }

    public EmptyTableRecords(TableMeta tableMeta) {
        this.setTableMeta(tableMeta);
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public List<Map<String, Field>> pkRows() {
        return new ArrayList<>();
    }

    @Override
    public void add(Row row) {
        throw new UnsupportedOperationException("xxx");
    }

    @Override
    public TableMeta getTableMeta() {
        throw new UnsupportedOperationException("xxx");
    }
}
