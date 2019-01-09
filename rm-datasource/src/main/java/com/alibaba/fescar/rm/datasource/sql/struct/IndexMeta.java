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

	public IndexMeta() {
	}

	public List<ColumnMeta> getValues() {
		return values;
	}

	public void setValues(List<ColumnMeta> values) {
		this.values = values;
	}

	public boolean isNonUnique() {
		return nonUnique;
	}

	public void setNonUnique(boolean nonUnique) {
		this.nonUnique = nonUnique;
	}

	public String getIndexQualifier() {
		return indexQualifier;
	}

	public void setIndexQualifier(String indexQualifier) {
		this.indexQualifier = indexQualifier;
	}

	public String getIndexName() {
		return indexName;
	}

	public void setIndexName(String indexName) {
		this.indexName = indexName;
	}

	public short getType() {
		return type;
	}

	public void setType(short type) {
		this.type = type;
	}

	public String getAscOrDesc() {
		return ascOrDesc;
	}

	public void setAscOrDesc(String ascOrDesc) {
		this.ascOrDesc = ascOrDesc;
	}

	public int getCardinality() {
		return cardinality;
	}

	public void setCardinality(int cardinality) {
		this.cardinality = cardinality;
	}

	public int getOrdinalPosition() {
		return ordinalPosition;
	}

	public void setOrdinalPosition(int ordinalPosition) {
		this.ordinalPosition = ordinalPosition;
	}

	public IndexType getIndextype() {
		return indextype;
	}

	public void setIndextype(IndexType indextype) {
		this.indextype = indextype;
	}

	public List<ColumnMeta> getIndexvalue() {
		return values;
	}

	@Override
	public String toString() {
		return "indexName:" + indexName + "->" + "type:" + type + "->" + "values:" + values;
	}
}
