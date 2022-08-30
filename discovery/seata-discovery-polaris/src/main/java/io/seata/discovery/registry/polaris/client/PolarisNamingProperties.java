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
package io.seata.discovery.registry.polaris.client;

import io.seata.common.util.StringUtils;

/**
 * {@link PolarisNamingProperties} Definition .
 *
 * @author <a href="mailto:iskp.me@gmail.com">Palmer Xu</a> 2022-08-18
 */
public class PolarisNamingProperties {

	/**
	 * Remote Polaris Config Server Address .
	 */
	private String address;

	/**
	 * Remote Polaris Config Server Access Token .
	 */
	private String token;

	/**
	 * Request Connect Timeout , default value : 6000 (ms) .
	 */
	private int connectTimeout = 6000;

	/**
	 * Request's response read timeout , default value : 5000 (ms) .
	 */
	private int readTimeout = 5000;

	/**
	 * Remote Service Instance Refresh Time, default value : 2000 (ms).
	 */
	private int refreshTime = 2000;

	public PolarisNamingProperties() {
	}

	/**
	 * {@link  PolarisNamingProperties} Constructor
	 *
	 * @param address server address
	 * @param token   server api access token
	 */
	public PolarisNamingProperties(String address, String token) {
		this.address = address;
		this.token = token;
	}

	/**
	 * {@link  PolarisNamingProperties} Constructor
	 *
	 * @param address server address
	 * @param token   server api access token
	 * @param connectTimeout request connect timeout
	 * @param readTimeout response read timeout
	 */
	public PolarisNamingProperties(String address, String token, int connectTimeout,
			int readTimeout) {
		this.address = address;
		this.token = token;
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

	public PolarisNamingProperties address(String address) {
		this.address = address;
		return this;
	}

	public String token() {
		return token;
	}

	public PolarisNamingProperties token(String token) {
		this.token = token;
		return this;
	}

	public int connectTimeout() {
		return connectTimeout;
	}

	public PolarisNamingProperties connectTimeout(int connectTimeout) {
		this.connectTimeout = connectTimeout;
		return this;
	}

	public int readTimeout() {
		return readTimeout;
	}

	public PolarisNamingProperties readTimeout(int readTimeout) {
		this.readTimeout = readTimeout;
		return this;
	}

	public int refreshTime() {
		return refreshTime;
	}

	public PolarisNamingProperties refreshTime(int refreshTime) {
		this.refreshTime = refreshTime;
		return this;
	}
}
