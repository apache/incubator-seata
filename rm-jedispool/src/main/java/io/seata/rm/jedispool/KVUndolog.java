/*
 *  Copyright 1999-2019 Seata.io Group.
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
package io.seata.rm.jedispool;

/**
 * @author funkye
 */
public class KVUndolog {
	private String key;

	private String method;

	private String beforeValue;

	private String afterValue;

	public KVUndolog() {
	}

    public KVUndolog(String key, String beforeValue, String afterValue, String method) {
        this.key = key;
        this.beforeValue = beforeValue;
        this.afterValue = afterValue;
        this.method = method;
    }

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getBeforeValue() {
		return beforeValue;
	}

	public void setBeforeValue(String beforeValue) {
		this.beforeValue = beforeValue;
	}

	public String getAfterValue() {
		return afterValue;
	}

	public void setAfterValue(String afterValue) {
		this.afterValue = afterValue;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	/**
	 * The enum redis method name
	 */
    public enum RedisMethod {
		/**
		 * set
		 */
        set("set"),
		/**
		 * rpush
		 */
		rpush("rpush"),
		/**
		 * hget
		 */
		hset("hget");

		/**
		 * method
		 */
        final String method;

		/**
		 * @param method method
		 */
        RedisMethod(String method) {
            this.method = method;
        }
    }
	
}
