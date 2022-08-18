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
package io.seata.config.polaris.client;

import io.seata.common.util.StringUtils;

/**
 * {@link PolarisConfigProperties} Definition .
 *
 * @author <a href="mailto:iskp.me@gmail.com">Palmer Xu</a> 2022-08-18
 */
public class PolarisConfigProperties {

	/**
	 * Remote Polaris Config Server Address .
	 */
	private String address;

	/**
	 * Remote Polaris Config Server Access Token .
	 */
	private String token;

	/**
	 * Remote config pull interval, default value : 5000 (ms) .
	 */
	private long pullIntervalTime = 5000L;

	/**
	 * Request Connect Timeout , default value : 6000 (ms) .
	 */
	private int connectTimeout = 6000;

	/**
	 * Request's response read timeout , default value : 5000 (ms) .
	 */
	private int readTimeout = 5000;

	public PolarisConfigProperties() {
	}

	/**
	 * {@link  PolarisConfigProperties} Constructor
	 *
	 * @param address server address
	 * @param token   server api access token
	 */
	public PolarisConfigProperties(String address, String token) {
		this.address = address;
		this.token = token;
	}

	public PolarisConfigProperties(String address, String token, long pullIntervalTime, int connectTimeout,
			int readTimeout) {
		this.address = address;
		this.token = token;
		this.pullIntervalTime = pullIntervalTime;
		this.connectTimeout = connectTimeout;
		this.readTimeout = readTimeout;
	}

	public String address() {
		if (StringUtils.isNotBlank(this.address)) {
			if (this.address.endsWith("/")) {
				this.address = this.address.substring(0, this.address.length() - 1);
			}
		}
		return address;
	}

	public PolarisConfigProperties address(String address) {
		this.address = address;
		return this;
	}

	public String token() {
		return token;
	}

	public PolarisConfigProperties token(String token) {
		this.token = token;
		return this;
	}

	public long pullIntervalTime() {
		return pullIntervalTime;
	}

	public PolarisConfigProperties pullIntervalTime(long pullIntervalTime) {
		this.pullIntervalTime = pullIntervalTime;
		return this;
	}

	public int connectTimeout() {
		return connectTimeout;
	}

	public PolarisConfigProperties connectTimeout(int connectTimeout) {
		this.connectTimeout = connectTimeout;
		return this;
	}

	public int readTimeout() {
		return readTimeout;
	}

	public PolarisConfigProperties readTimeout(int readTimeout) {
		this.readTimeout = readTimeout;
		return this;
	}
}
