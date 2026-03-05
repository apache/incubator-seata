/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
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

package com.alibaba.fescar.rm.datasource.sql;

/**
 * The enum Sql type.
 */
public enum SQLType {

    /**
     * Select sql type.
     */
    SELECT(0),
    /**
     * Insert sql type.
     */
    INSERT(1),
    /**
     * Update sql type.
     */
    UPDATE(2),
    /**
     * Delete sql type.
     */
    DELETE(3),
    /**
     * Select for update sql type.
     */
    SELECT_FOR_UPDATE(4),
    /**
     * Replace sql type.
     */
    REPLACE(5),
    /**
     * Truncate sql type.
     */
    TRUNCATE(6),
    /**
     * Create sql type.
     */
    CREATE(7),
    /**
     * Drop sql type.
     */
    DROP(8),
    /**
     * Load sql type.
     */
    LOAD(9),
    /**
     * Merge sql type.
     */
    MERGE(10),
    /**
     * Show sql type.
     */
    SHOW(11),
    /**
     * Alter sql type.
     */
    ALTER(12),
    /**
     * Rename sql type.
     */
    RENAME(13),
    /**
     * Dump sql type.
     */
    DUMP(14),
    /**
     * Debug sql type.
     */
    DEBUG(15),
    /**
     * Explain sql type.
     */
    EXPLAIN(16),
    /**
     * 存储过程
     */
    PROCEDURE(17),
    /**
     * Desc sql type.
     */
    DESC(18),
    /**
     * 获取上一个insert id
     */
    SELECT_LAST_INSERT_ID(19),
    /**
     * Select without table sql type.
     */
    SELECT_WITHOUT_TABLE(20),
    /**
     * Create sequence sql type.
     */
    CREATE_SEQUENCE(21),
    /**
     * Show sequences sql type.
     */
    SHOW_SEQUENCES(22),
    /**
     * Get sequence sql type.
     */
    GET_SEQUENCE(23),
    /**
     * Alter sequence sql type.
     */
    ALTER_SEQUENCE(24),
    /**
     * Drop sequence sql type.
     */
    DROP_SEQUENCE(25),
    /**
     * Tddl show sql type.
     */
    TDDL_SHOW(26),
    /**
     * Set sql type.
     */
    SET(27),
    /**
     * Reload sql type.
     */
    RELOAD(28),
    /**
     * Select union sql type.
     */
    SELECT_UNION(29),
    /**
     * Create table sql type.
     */
    CREATE_TABLE(30),
    /**
     * Drop table sql type.
     */
    DROP_TABLE(31),
    /**
     * Alter table sql type.
     */
    ALTER_TABLE(32),
    /**
     * Save point sql type.
     */
    SAVE_POINT(33),
    /**
     * Select from update sql type.
     */
    SELECT_FROM_UPDATE(34),
    /**
     * multi delete/update
     */
    MULTI_DELETE(35),
    /**
     * Multi update sql type.
     */
    MULTI_UPDATE(36),
    /**
     * Create index sql type.
     */
    CREATE_INDEX(37),
    /**
     * Drop index sql type.
     */
    DROP_INDEX(38),
    /**
     * Kill sql type.
     */
    KILL(39),
    /**
     * Release dblock sql type.
     */
    RELEASE_DBLOCK(40),
    /**
     * Lock tables sql type.
     */
    LOCK_TABLES(41),
    /**
     * Unlock tables sql type.
     */
    UNLOCK_TABLES(42),
    /**
     * Check table sql type.
     */
    CHECK_TABLE(43),

    /**
     * 获取上sql_calc_found_rows 的结果
     */
    SELECT_FOUND_ROWS(44),

    /**
     * Insert ingore sql type.
     */
    // FESCAR
    INSERT_INGORE(101),
    /**
     * Insert on duplicate update sql type.
     */
    INSERT_ON_DUPLICATE_UPDATE(102);

    private int i;

    private SQLType(int i){
        this.i = i;
    }

    /**
     * Value int.
     *
     * @return the int
     */
    public int value() {
        return this.i;
    }

    /**
     * Value of sql type.
     *
     * @param i the
     * @return the sql type
     */
    public static SQLType valueOf(int i) {
        for (SQLType t : values()) {
            if (t.value() == i) {
                return t;
            }
        }
        throw new IllegalArgumentException("Invalid SQLType:" + i);
    }
}
