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
package com.alibaba.fescar.rm.datasource.sql.struct;

import java.util.ArrayList;
import java.util.List;

/**
 * The type Index meta.
 */
public class IndexMeta {
    private List<ColumnMeta> values = new ArrayList<ColumnMeta>();

    private boolean nonUnique;
    private String indexQualifier;
    private String indexName;
    private short type;
    private IndexType indextype;
    private String ascOrDesc;
    private int cardinality;
    private int ordinalPosition;

    /**
     * Instantiates a new Index meta.
     */
    public IndexMeta() {
    }

    /**
     * Gets values.
     *
     * @return the values
     */
    public List<ColumnMeta> getValues() {
        return values;
    }

    /**
     * Sets values.
     *
     * @param values the values
     */
    public void setValues(List<ColumnMeta> values) {
        this.values = values;
    }

    /**
     * Is non unique boolean.
     *
     * @return the boolean
     */
    public boolean isNonUnique() {
        return nonUnique;
    }

    /**
     * Sets non unique.
     *
     * @param nonUnique the non unique
     */
    public void setNonUnique(boolean nonUnique) {
        this.nonUnique = nonUnique;
    }

    /**
     * Gets index qualifier.
     *
     * @return the index qualifier
     */
    public String getIndexQualifier() {
        return indexQualifier;
    }

    /**
     * Sets index qualifier.
     *
     * @param indexQualifier the index qualifier
     */
    public void setIndexQualifier(String indexQualifier) {
        this.indexQualifier = indexQualifier;
    }

    /**
     * Gets index name.
     *
     * @return the index name
     */
    public String getIndexName() {
        return indexName;
    }

    /**
     * Sets index name.
     *
     * @param indexName the index name
     */
    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }

    /**
     * Gets type.
     *
     * @return the type
     */
    public short getType() {
        return type;
    }

    /**
     * Sets type.
     *
     * @param type the type
     */
    public void setType(short type) {
        this.type = type;
    }

    /**
     * Gets asc or desc.
     *
     * @return the asc or desc
     */
    public String getAscOrDesc() {
        return ascOrDesc;
    }

    /**
     * Sets asc or desc.
     *
     * @param ascOrDesc the asc or desc
     */
    public void setAscOrDesc(String ascOrDesc) {
        this.ascOrDesc = ascOrDesc;
    }

    /**
     * Gets cardinality.
     *
     * @return the cardinality
     */
    public int getCardinality() {
        return cardinality;
    }

    /**
     * Sets cardinality.
     *
     * @param cardinality the cardinality
     */
    public void setCardinality(int cardinality) {
        this.cardinality = cardinality;
    }

    /**
     * Gets ordinal position.
     *
     * @return the ordinal position
     */
    public int getOrdinalPosition() {
        return ordinalPosition;
    }

    /**
     * Sets ordinal position.
     *
     * @param ordinalPosition the ordinal position
     */
    public void setOrdinalPosition(int ordinalPosition) {
        this.ordinalPosition = ordinalPosition;
    }

    /**
     * Gets indextype.
     *
     * @return the indextype
     */
    public IndexType getIndextype() {
        return indextype;
    }

    /**
     * Sets indextype.
     *
     * @param indextype the indextype
     */
    public void setIndextype(IndexType indextype) {
        this.indextype = indextype;
    }

    /**
     * Gets indexvalue.
     *
     * @return the indexvalue
     */
    public List<ColumnMeta> getIndexvalue() {
        return values;
    }

    @Override
    public String toString() {
        return "indexName:" + indexName + "->" + "type:" + type + "->" + "values:" + values;
    }
}
