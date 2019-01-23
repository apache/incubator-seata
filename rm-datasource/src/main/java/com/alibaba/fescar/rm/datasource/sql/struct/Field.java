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

import java.util.Objects;

/**
 * Field
 */
public class Field {

	public String name;

	private KeyType keyType = KeyType.NULL;

	public int type;

	public Object value;

	public Field() {
	}

	public Field(String name, int type, Object value) {
		this.name = name;
		this.type = type;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public void setName(String attrName) {
		this.name = attrName;
	}

	public KeyType getKeyType() {
		return keyType;
	}

	public void setKeyType(KeyType keyType) {
		this.keyType = keyType;
	}

	public int getType() {
		return type;
	}

	public void setType(int attrType) {
		this.type = attrType;
	}

	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
	}

	public boolean isKey(String pkname) {
		return name.equalsIgnoreCase(pkname);
	}

	@Override
	public String toString() {
		return String.format("[%s,%s]", name, String.valueOf(value));
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Field field = (Field) o;
		if (type != field.type || !Objects.equals(name, field.name)) return false;
		if (value.getClass() == field.value.getClass()) {
			return Objects.equals(value, field.value);
		}
		return String.valueOf(value).equals(String.valueOf(field.value));
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, type, String.valueOf(value));
	}
}
