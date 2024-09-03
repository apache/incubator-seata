/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The Net Validator util test.
 *
 */
public class NetAddressValidatorUtilTest {

    @Test
    public void isIPv6Address() {
        assertThat(NetAddressValidatorUtil.isIPv6Address("2000:0000:0000:0000:0001:2345:6789:abcd")).isTrue();
        assertThat(NetAddressValidatorUtil.isIPv6Address("2001:DB8:0:0:8:800:200C:417A")).isTrue();
        assertThat(NetAddressValidatorUtil.isIPv6Address("2001:DB8::8:800:200C:417A")).isTrue();
        assertThat(NetAddressValidatorUtil.isIPv6Address("2001:DB8::8:800:200C141aA")).isFalse();
        assertThat(NetAddressValidatorUtil.isIPv6Address("::")).isTrue();
    }

    @Test
    public void isIPv6MixedAddress() {
        assertThat(NetAddressValidatorUtil.isIPv6MixedAddress("1:0:0:0:0:0:172.12.55.18")).isTrue();
        assertThat(NetAddressValidatorUtil.isIPv6MixedAddress("2001:DB8::8:800:200C141aA")).isFalse();
    }

    @Test
    public void isIPv6IPv4MappedAddress() {
        assertThat(NetAddressValidatorUtil.isIPv6IPv4MappedAddress(":ffff:1.1.1.1")).isFalse();
        assertThat(NetAddressValidatorUtil.isIPv6IPv4MappedAddress("::FFFF:192.168.1.2")).isTrue();
    }

    @Test
    public void isIPv4Address() {
        assertThat(NetAddressValidatorUtil.isIPv4Address("192.168.1.2")).isTrue();
        assertThat(NetAddressValidatorUtil.isIPv4Address("127.0.0.1")).isTrue();
        assertThat(NetAddressValidatorUtil.isIPv4Address("999.999.999.999")).isFalse();
    }
}
