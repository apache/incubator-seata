package io.seata.rm.datasource.exec;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * @author jingliu_xiong@foxmail.com
 */
public interface ReplaceExecutor<T> extends Executor<T> {
    /**
     * get primary key values.
     *
     * @return The primary key value.
     * @throws SQLException the sql exception
     */
    Map<String, List<Object>> getPkValues() throws SQLException;

    /**
     * get primary key values by insert column.
     *
     * @return pk values by column
     * @throws SQLException the sql exception
     */
    Map<String, List<Object>> getPkValuesByColumn() throws SQLException;
}
