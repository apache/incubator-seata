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

package com.alibaba.fescar.rm.datasource.exec;

import com.alibaba.fescar.config.ConfigurationFactory;
import com.alibaba.fescar.core.service.ConfigurationKeys;

public class LockRetryController {

	private static int LOCK_RETRY_INTERNAL =
		ConfigurationFactory.getInstance().getInt(ConfigurationKeys.CLIENT_LOCK_RETRY_INTERNAL, 10);
	private static int LOCK_RETRY_TIMES =
		ConfigurationFactory.getInstance().getInt(ConfigurationKeys.CLIENT_LOCK_RETRY_TIMES, 30);

	private int lockRetryInternal = LOCK_RETRY_INTERNAL;
	private int lockRetryTimes = LOCK_RETRY_TIMES;

	public LockRetryController() {
	}

	public void sleep(Exception e) throws LockWaitTimeoutException {
		if (--lockRetryTimes < 0) {
			throw new LockWaitTimeoutException("Global lock wait timeout", e);
		}

		try {
			Thread.sleep(lockRetryInternal);
		} catch (InterruptedException ignore) {
		}
	}
}