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
package io.seata.server.storage.r2dbc.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @author jianbin.chen
 */
@Table("distributed_lock")
public class DistributedLock {
	/**
	 * the key of distributed lock
	 */
	@Id
	private String lockKey;
	/**
	 * the value of distributed lock
	 */
	private String lockValue;
	/**
	 * the expire time of distributed lock,time unit is milliseconds
	 */
	private Long expireTime;

	public DistributedLock() {
	}

	public DistributedLock(String lockKey, String lockValue, Long expireTime) {
		this.lockKey = lockKey;
		this.lockValue = lockValue;
		this.expireTime = expireTime;
	}

	public String getLockKey() {
		return lockKey;
	}

	public void setLockKey(String lockKey) {
		this.lockKey = lockKey;
	}

	public String getLockValue() {
		return lockValue;
	}

	public void setLockValue(String lockValue) {
		this.lockValue = lockValue;
	}

	public Long getExpireTime() {
		return expireTime;
	}

	public void setExpireTime(Long expireTime) {
		this.expireTime = expireTime;
	}
}
