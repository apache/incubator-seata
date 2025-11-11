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
        assertThat(NetAddressValidatorUtil.isIPv6Address("2000:0000:0000:0000:0001:2345:6789:abcd"))
                .isTrue();
        assertThat(NetAddressValidatorUtil.isIPv6Address("2001:DB8:0:0:8:800:200C:417A"))
                .isTrue();
        assertThat(NetAddressValidatorUtil.isIPv6Address("2001:DB8::8:800:200C:417A"))
                .isTrue();
        assertThat(NetAddressValidatorUtil.isIPv6Address("2001:DB8::8:800:200C141aA"))
                .isFalse();
        assertThat(NetAddressValidatorUtil.isIPv6Address("::")).isTrue();
    }

    @Test
    public void isIPv6MixedAddress() {
        assertThat(NetAddressValidatorUtil.isIPv6MixedAddress("1:0:0:0:0:0:172.12.55.18"))
                .isTrue();
        assertThat(NetAddressValidatorUtil.isIPv6MixedAddress("2001:DB8::8:800:200C141aA"))
                .isFalse();
    }

    @Test
    public void isIPv6IPv4MappedAddress() {
        assertThat(NetAddressValidatorUtil.isIPv6IPv4MappedAddress(":ffff:1.1.1.1"))
                .isFalse();
        assertThat(NetAddressValidatorUtil.isIPv6IPv4MappedAddress("::FFFF:192.168.1.2"))
                .isTrue();
    }

    @Test
    public void isIPv4Address() {
        assertThat(NetAddressValidatorUtil.isIPv4Address("192.168.1.2")).isTrue();
        assertThat(NetAddressValidatorUtil.isIPv4Address("127.0.0.1")).isTrue();
        assertThat(NetAddressValidatorUtil.isIPv4Address("999.999.999.999")).isFalse();
    }

    @Test
    public void isLinkLocalIPv6WithZoneIndex() {
        assertThat(NetAddressValidatorUtil.isLinkLocalIPv6WithZoneIndex("2409:8a5c:6730:4490:f0e8:b9ad:3b3d:e739%br0"))
                .isTrue();
        assertThat(NetAddressValidatorUtil.isLinkLocalIPv6WithZoneIndex("2409:8a5c:6730:4490:f0e8:b9ad:3b3d:e739%"))
                .isFalse();
    }

    @Test
    public void testIsIPv4AddressWithValidAddresses() {
        // Test standard IPv4 addresses
        assertThat(NetAddressValidatorUtil.isIPv4Address("192.168.1.1")).isTrue();
        assertThat(NetAddressValidatorUtil.isIPv4Address("0.0.0.0")).isTrue();
        assertThat(NetAddressValidatorUtil.isIPv4Address("255.255.255.255")).isTrue();
        assertThat(NetAddressValidatorUtil.isIPv4Address("127.0.0.1")).isTrue();
        assertThat(NetAddressValidatorUtil.isIPv4Address("8.8.8.8")).isTrue();
    }

    @Test
    public void testIsIPv4AddressWithInvalidAddresses() {
        // Test invalid IPv4 addresses
        assertThat(NetAddressValidatorUtil.isIPv4Address("256.1.1.1")).isFalse();
        assertThat(NetAddressValidatorUtil.isIPv4Address("192.168.1")).isFalse();
        assertThat(NetAddressValidatorUtil.isIPv4Address("192.168.1.1.1")).isFalse();
        assertThat(NetAddressValidatorUtil.isIPv4Address("192.168.-1.1")).isFalse();
        assertThat(NetAddressValidatorUtil.isIPv4Address("192.168.1.256")).isFalse();
        assertThat(NetAddressValidatorUtil.isIPv4Address("not.an.ip.address")).isFalse();
        assertThat(NetAddressValidatorUtil.isIPv4Address("")).isFalse();
    }

    @Test
    public void testIsIPv6StdAddress() {
        // Test standard IPv6 addresses
        assertThat(NetAddressValidatorUtil.isIPv6StdAddress("2001:0db8:85a3:0000:0000:8a2e:0370:7334"))
                .isTrue();
        assertThat(NetAddressValidatorUtil.isIPv6StdAddress("2001:db8:85a3:0:0:8a2e:370:7334"))
                .isTrue();
        assertThat(NetAddressValidatorUtil.isIPv6StdAddress("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff"))
                .isTrue();
        assertThat(NetAddressValidatorUtil.isIPv6StdAddress("0000:0000:0000:0000:0000:0000:0000:0001"))
                .isTrue();

        // Test invalid standard IPv6 addresses
        assertThat(NetAddressValidatorUtil.isIPv6StdAddress("2001:0db8:85a3:0000:0000:8a2e:0370"))
                .isFalse(); // Too short
        assertThat(NetAddressValidatorUtil.isIPv6StdAddress("2001:0db8:85a3:0000:0000:8a2e:0370:7334:extra"))
                .isFalse(); // Too long
        assertThat(NetAddressValidatorUtil.isIPv6StdAddress("2001:0gb8:85a3:0000:0000:8a2e:0370:7334"))
                .isFalse(); // Invalid hex
    }

    @Test
    public void testIsIPv6HexCompressedAddress() {
        // Test compressed IPv6 addresses
        assertThat(NetAddressValidatorUtil.isIPv6HexCompressedAddress("2001:db8:85a3::8a2e:370:7334"))
                .isTrue();
        assertThat(NetAddressValidatorUtil.isIPv6HexCompressedAddress("2001:db8:85a3:0:0:8a2e::"))
                .isTrue();
        assertThat(NetAddressValidatorUtil.isIPv6HexCompressedAddress("::1")).isTrue();
        assertThat(NetAddressValidatorUtil.isIPv6HexCompressedAddress("::")).isTrue();
        assertThat(NetAddressValidatorUtil.isIPv6HexCompressedAddress("::ffff:c000:280"))
                .isTrue();

        // Test invalid compressed IPv6 addresses
        assertThat(NetAddressValidatorUtil.isIPv6HexCompressedAddress("2001::db8::85a3"))
                .isFalse(); // Multiple ::
    }

    @Test
    public void testIsIPv6MixedAddressEnhanced() {
        // Test IPv6 mixed addresses
        assertThat(NetAddressValidatorUtil.isIPv6MixedAddress("::192.168.1.1")).isTrue();
        assertThat(NetAddressValidatorUtil.isIPv6MixedAddress("::ffff:192.168.1.1"))
                .isTrue();
        assertThat(NetAddressValidatorUtil.isIPv6MixedAddress("2001:db8:85a3::192.168.1.1"))
                .isTrue();
        assertThat(NetAddressValidatorUtil.isIPv6MixedAddress("::ffff:0:0:192.168.1.1"))
                .isTrue();

        // Test invalid IPv6 mixed addresses
        assertThat(NetAddressValidatorUtil.isIPv6MixedAddress("::192.168.1.256"))
                .isFalse(); // Invalid IPv4 part
        assertThat(NetAddressValidatorUtil.isIPv6MixedAddress("::192.168.1")).isFalse(); // Incomplete IPv4 part
        assertThat(NetAddressValidatorUtil.isIPv6MixedAddress("2001:db8:85a3:192.168.1.1"))
                .isFalse(); // Missing ::
    }

    @Test
    public void testIsIPv6IPv4MappedAddressEnhanced() {
        // Test IPv4-mapped IPv6 addresses
        assertThat(NetAddressValidatorUtil.isIPv6IPv4MappedAddress("::ffff:192.168.1.1"))
                .isTrue();
        assertThat(NetAddressValidatorUtil.isIPv6IPv4MappedAddress("::ffff:127.0.0.1"))
                .isTrue();
        assertThat(NetAddressValidatorUtil.isIPv6IPv4MappedAddress("::FFFF:8.8.8.8"))
                .isTrue();

        // Test invalid IPv4-mapped IPv6 addresses
        assertThat(NetAddressValidatorUtil.isIPv6IPv4MappedAddress("::fff:192.168.1.1"))
                .isFalse(); // Missing 'f'
        assertThat(NetAddressValidatorUtil.isIPv6IPv4MappedAddress("::ffff:192.168.1.256"))
                .isFalse(); // Invalid IPv4
        assertThat(NetAddressValidatorUtil.isIPv6IPv4MappedAddress("ffff:192.168.1.1"))
                .isFalse(); // Missing ::
    }

    @Test
    public void testIsLinkLocalIPv6WithZoneIndexEnhanced() {
        // Test link-local IPv6 with zone index
        assertThat(NetAddressValidatorUtil.isLinkLocalIPv6WithZoneIndex("fe80::1%eth0"))
                .isTrue();
        assertThat(NetAddressValidatorUtil.isLinkLocalIPv6WithZoneIndex("fe80::1%1"))
                .isTrue();
        assertThat(NetAddressValidatorUtil.isLinkLocalIPv6WithZoneIndex("fe80::abcd:1234:ef56%en0"))
                .isTrue();

        // Test invalid link-local IPv6 with zone index
        assertThat(NetAddressValidatorUtil.isLinkLocalIPv6WithZoneIndex("fe80::1%"))
                .isFalse(); // Empty zone index
        assertThat(NetAddressValidatorUtil.isLinkLocalIPv6WithZoneIndex("fe80::1"))
                .isFalse(); // No zone index
    }

    @Test
    public void testIsIPv6AddressEnhanced() {
        // Test valid IPv6 addresses of all types
        assertThat(NetAddressValidatorUtil.isIPv6Address("2001:0db8:85a3:0000:0000:8a2e:0370:7334"))
                .isTrue(); // Standard
        assertThat(NetAddressValidatorUtil.isIPv6Address("2001:db8:85a3::8a2e:370:7334"))
                .isTrue(); // Compressed
        assertThat(NetAddressValidatorUtil.isIPv6Address("::1")).isTrue(); // Loopback
        assertThat(NetAddressValidatorUtil.isIPv6Address("::")).isTrue(); // Unspecified
        assertThat(NetAddressValidatorUtil.isIPv6Address("fe80::1%eth0")).isTrue(); // Link-local with zone
        assertThat(NetAddressValidatorUtil.isIPv6Address("::ffff:192.168.1.1")).isTrue(); // IPv4-mapped
        assertThat(NetAddressValidatorUtil.isIPv6Address("::192.168.1.1")).isTrue(); // IPv4-embedded

        // Test invalid IPv6 addresses
        assertThat(NetAddressValidatorUtil.isIPv6Address("2001:0db8:85a3::8a2e::7334"))
                .isFalse(); // Multiple ::
        assertThat(NetAddressValidatorUtil.isIPv6Address("gggg::1")).isFalse(); // Invalid hex
        assertThat(NetAddressValidatorUtil.isIPv6Address("")).isFalse(); // Empty
    }
}
