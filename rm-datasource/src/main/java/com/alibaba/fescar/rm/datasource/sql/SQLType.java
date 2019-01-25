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

public enum SQLType {

    /** */
    SELECT(0),
    /** */
    INSERT(1),
    /** */
    UPDATE(2),
    /** */
    DELETE(3),
    /** */
    SELECT_FOR_UPDATE(4),
    /** */
    REPLACE(5),
    /** */
    TRUNCATE(6),
    /** */
    CREATE(7),
    /** */
    DROP(8),
    /** */
    LOAD(9),
    /** */
    MERGE(10),
    /** */
    SHOW(11),
    /** */
    ALTER(12),
    /** */
    RENAME(13),
    /** */
    DUMP(14),
    /** */
    DEBUG(15),
    /** */
    EXPLAIN(16),
    /** 存储过程 */
    PROCEDURE(17),
    /** */
    DESC(18),
    /** 获取上一个insert id */
    SELECT_LAST_INSERT_ID(19),
    /** */
    SELECT_WITHOUT_TABLE(20),
    /** */
    CREATE_SEQUENCE(21), SHOW_SEQUENCES(22), GET_SEQUENCE(23), ALTER_SEQUENCE(24), DROP_SEQUENCE(25),
    /** */
    TDDL_SHOW(26),
    /** */
    SET(27), RELOAD(28),
    /** */
    SELECT_UNION(29),
    /** */
    CREATE_TABLE(30), DROP_TABLE(31), ALTER_TABLE(32), SAVE_POINT(33), SELECT_FROM_UPDATE(34),
    /** multi delete/update */
    MULTI_DELETE(35), MULTI_UPDATE(36),
    /** */
    CREATE_INDEX(37), DROP_INDEX(38), KILL(39), RELEASE_DBLOCK(40), LOCK_TABLES(41), UNLOCK_TABLES(42),
    CHECK_TABLE(43),

    /** 获取上sql_calc_found_rows 的结果 */
    SELECT_FOUND_ROWS(44),

    // FESCAR
    INSERT_INGORE(101),
    INSERT_ON_DUPLICATE_UPDATE(102)
    ;

    private int i;

    private SQLType(int i){
        this.i = i;
    }

    public int value() {
        return this.i;
    }

    public static SQLType valueOf(int i) {
        for (SQLType t : values()) {
            if (t.value() == i) {
                return t;
            }
        }
        throw new IllegalArgumentException("Invalid SQLType:" + i);
    }
}
