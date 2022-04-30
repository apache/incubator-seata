package io.seata.sqlparser;

import java.util.List;

/**
 * @author jingliu_xiong@foxmail.com
 */
public interface SQLReplaceRecognizer extends SQLRecognizer {
    /**
     * select query is empty.
     *
     * @return true: empty. false: not empty.
     */
    boolean selectQueryIsEmpty();

    /**
     * get replace columns.
     *
     * @return
     */
    List<String> getReplaceColumns();

    /**
     * get replace values.
     *
     * @return (?, ?, ?, ?, ?, ?, ?, ?, ?), (?, ?, ?, ?, ?, ?, ?, ?, ?)
     */
    List<String> getReplaceValues();

    /**
     * get select query
     *
     * @return if do not have select query, return ""
     */
    String getSelectQuery();
}
