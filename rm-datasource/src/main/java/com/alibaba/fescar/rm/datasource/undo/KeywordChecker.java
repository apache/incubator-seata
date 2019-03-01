package com.alibaba.fescar.rm.datasource.undo;
/**
 * @author Wu
 * xingfudeshi@gmail.com
 * The interface Keyword checker
 */
public interface KeywordChecker {
    /**
     * check whether given field name and table name use keywords
     * @param fieldOrTableName
     * @return
     */
    boolean check(String fieldOrTableName);

    /**
     * check whether given field name and table name use keywords and,if so,will add "`" to the name.
     * @param fieldOrTableName
     * @return
     */
    String checkAndReplace(String fieldOrTableName);
}
