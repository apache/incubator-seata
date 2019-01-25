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

public class ColumnMeta {
	private String tableCat;
	private String tableSchemaName;
	private String tableName;
	private String columnName;
	private int dataType;
	private String dataTypeName;
	private int columnSize;
	private int decimalDigits;
	private int numPrecRadix;
	private int nullAble;
	private String remarks;
	private String columnDef;
	private int sqlDataType;
	private int sqlDatetimeSub;
	private int charOctetLength;
	private int ordinalPosition;
	private String isNullAble;
	private String isAutoincrement;


	public ColumnMeta() {
	}

	@Override
	public String toString() {
		return "ColumnMeta{" +
			"tableCat='" + tableCat + '\'' +
			", tableSchemaName='" + tableSchemaName + '\'' +
			", tableName='" + tableName + '\'' +
			", columnName='" + columnName + '\'' +
			", dataType=" + dataType +
			", dataTypeName='" + dataTypeName + '\'' +
			", columnSize=" + columnSize +
			", decimalDigits=" + decimalDigits +
			", numPrecRadix=" + numPrecRadix +
			", nullAble=" + nullAble +
			", remarks='" + remarks + '\'' +
			", columnDef='" + columnDef + '\'' +
			", sqlDataType=" + sqlDataType +
			", sqlDatetimeSub=" + sqlDatetimeSub +
			", charOctetLength=" + charOctetLength +
			", ordinalPosition=" + ordinalPosition +
			", isNullAble='" + isNullAble + '\'' +
			", isAutoincrement='" + isAutoincrement + '\'' +
			'}';
	}

	public boolean isAutoincrement() {
		return "YES".equalsIgnoreCase(isAutoincrement);
	}

	public String getTableCat() {
		return tableCat;
	}

	public void setTableCat(String tableCat) {
		this.tableCat = tableCat;
	}

	public void setTableSchemaName(String tableSchemaName) {
		this.tableSchemaName = tableSchemaName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public String getColumnName() {
		return columnName;
	}

	public void setColumnName(String columnName) {
		this.columnName = columnName;
	}

	public int getDataType() {
		return dataType;
	}

	public void setDataType(int dataType) {
		this.dataType = dataType;
	}

	public String getDataTypeName() {
		return dataTypeName;
	}

	public void setDataTypeName(String dataTypeName) {
		this.dataTypeName = dataTypeName;
	}

	public int getColumnSize() {
		return columnSize;
	}

	public void setColumnSize(int columnSize) {
		this.columnSize = columnSize;
	}

	public int getDecimalDigits() {
		return decimalDigits;
	}

	public void setDecimalDigits(int decimalDigits) {
		this.decimalDigits = decimalDigits;
	}

	public int getNumPrecRadix() {
		return numPrecRadix;
	}

	public void setNumPrecRadix(int numPrecRadix) {
		this.numPrecRadix = numPrecRadix;
	}

	public int getNullAble() {
		return nullAble;
	}

	public void setNullAble(int nullAble) {
		this.nullAble = nullAble;
	}

	public String getRemarks() {
		return remarks;
	}

	public void setRemarks(String remarks) {
		this.remarks = remarks;
	}

	public String getColumnDef() {
		return columnDef;
	}

	public void setColumnDef(String columnDef) {
		this.columnDef = columnDef;
	}

	public int getSqlDataType() {
		return sqlDataType;
	}

	public void setSqlDataType(int sqlDataType) {
		this.sqlDataType = sqlDataType;
	}

	public int getSqlDatetimeSub() {
		return sqlDatetimeSub;
	}

	public void setSqlDatetimeSub(int sqlDatetimeSub) {
		this.sqlDatetimeSub = sqlDatetimeSub;
	}

	public int getCharOctetLength() {
		return charOctetLength;
	}

	public void setCharOctetLength(int charOctetLength) {
		this.charOctetLength = charOctetLength;
	}

	public int getOrdinalPosition() {
		return ordinalPosition;
	}

	public void setOrdinalPosition(int ordinalPosition) {
		this.ordinalPosition = ordinalPosition;
	}

	public String getIsNullAble() {
		return isNullAble;
	}

	public void setIsNullAble(String isNullAble) {
		this.isNullAble = isNullAble;
	}

	public String getIsAutoincrement() {
		return isAutoincrement;
	}

	public void setIsAutoincrement(String isAutoincrement) {
		this.isAutoincrement = isAutoincrement;
	}
}
