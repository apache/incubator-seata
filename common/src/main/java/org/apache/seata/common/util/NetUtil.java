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

import org.apache.seata.common.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;


/**
 * The type Net util.
 *
 */
public class NetUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(NetUtil.class);

    public static final boolean PREFER_IPV6_ADDRESSES = Boolean.parseBoolean(
            System.getProperty("java.net.preferIPv6Addresses"));

    private static final String LOCALHOST = "127.0.0.1";
    private static final String ANY_HOST = "0.0.0.0";

    public static final String LOCALHOST_IPV6 = "0:0:0:0:0:0:0:1";
    public static final String LOCALHOST_SHORT_IPV6 = "::1";
    public static final String ANY_HOST_IPV6  = "0:0:0:0:0:0:0:0";
    public static final String ANY_HOST_SHORT_IPV6  = "::";

    private static volatile InetAddress LOCAL_ADDRESS = null;

    private static final Set<String> FORBIDDEN_HOSTS = Collections.unmodifiableSet(
            new LinkedHashSet<>(Arrays.asList(
                            LOCALHOST, ANY_HOST,
                            LOCALHOST_IPV6, LOCALHOST_SHORT_IPV6,
                            ANY_HOST_IPV6,ANY_HOST_SHORT_IPV6)));

    /**
     * To string address string.
     *
     * @param address the address
     * @return the string
     */
    public static String toStringAddress(SocketAddress address) {
        if (address == null) {
            return StringUtils.EMPTY;
        }
        return toStringAddress((InetSocketAddress) address);
    }

    /**
     * To ip address string.
     *
     * @param address the address
     * @return the string
     */
    public static String toIpAddress(SocketAddress address) {
        InetSocketAddress inetSocketAddress = (InetSocketAddress) address;
        return inetSocketAddress.getAddress().getHostAddress();
    }

    /**
     * To string address string.
     *
     * @param address the address
     * @return the string
     */
    public static String toStringAddress(InetSocketAddress address) {
        return address.getAddress().getHostAddress() + ":" + address.getPort();
    }

    /**
     * To inet socket address inet socket address.
     *
     * @param address the address
     * @return the inet socket address
     */
    public static InetSocketAddress toInetSocketAddress(String address) {
        String[] ipPortStr = splitIPPortStr(address);
        String host;
        int port;
        if (null != ipPortStr) {
            host = ipPortStr[0];
            port = Integer.parseInt(ipPortStr[1]);
        } else {
            host = address;
            port = 0;
        }
        return new InetSocketAddress(host, port);
    }

    public static String[] splitIPPortStr(String address) {
        if (StringUtils.isBlank(address)) {
            throw new IllegalArgumentException("ip and port string cannot be empty!");
        }
        if (address.charAt(0) == '[') {
            address = removeBrackets(address);
        }
        String[] serverAddArr = null;
        int i = address.lastIndexOf(Constants.IP_PORT_SPLIT_CHAR);
        if (i > -1) {
            serverAddArr = new String[2];
            String hostAddress = address.substring(0,i);
            if (hostAddress.contains("%")) {
                hostAddress = hostAddress.substring(0, hostAddress.indexOf("%"));
            }
            serverAddArr[0] = hostAddress;
            serverAddArr[1] = address.substring(i + 1);
        }
        return serverAddArr;
    }

    /**
     * To long long.
     *
     * @param address the address
     * @return the long
     */
    public static long toLong(String address) {
        InetSocketAddress ad = toInetSocketAddress(address);
        String[] ip = ad.getAddress().getHostAddress().split("\\.");
        long r = 0;
        r = r | (Long.parseLong(ip[0]) << 40);
        r = r | (Long.parseLong(ip[1]) << 32);
        r = r | (Long.parseLong(ip[2]) << 24);
        r = r | (Long.parseLong(ip[3]) << 16);
        r = r | ad.getPort();
        return r;
    }

    /**
     * Gets local ip.
     *
     * @return the local ip
     */
    public static String getLocalIp(String... preferredNetworks) {
        InetAddress address = getLocalAddress(preferredNetworks);
        if (null != address) {
            String hostAddress = address.getHostAddress();
            if (address instanceof Inet6Address) {
                if (hostAddress.contains("%")) {
                    hostAddress = hostAddress.substring(0, hostAddress.indexOf("%"));
                }
            }
            return hostAddress;
        }
        return localIP();
    }

    public static String localIP() {
        if (PREFER_IPV6_ADDRESSES) {
            return LOCALHOST_IPV6;
        }
        return LOCALHOST;
    }

    /**
     * Gets local host.
     *
     * @return the local host
     */
    public static String getLocalHost() {
        InetAddress address = getLocalAddress();
        return address == null ? "localhost" : address.getHostName();
    }

    /**
     * Gets local address.
     * not support ipv6
     * if match the preferredNetworks rule return the first
     * if all not match preferredNetworks rule return the first valid ip
     * @return the local address
     */
    public static InetAddress getLocalAddress(String... preferredNetworks) {
        if (LOCAL_ADDRESS != null) {
            return LOCAL_ADDRESS;
        }
        InetAddress localAddress = getLocalAddress0(preferredNetworks);
        LOCAL_ADDRESS = localAddress;
        return localAddress;
    }

    private static InetAddress getLocalAddress0(String... preferredNetworks) {
        InetAddress localAddress = null;
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            if (interfaces != null) {
                while (interfaces.hasMoreElements()) {
                    try {
                        NetworkInterface network = interfaces.nextElement();
                        if (network.isUp()) {
                            Enumeration<InetAddress> addresses = network.getInetAddresses();
                            while (addresses.hasMoreElements()) {
                                try {
                                    InetAddress address = addresses.nextElement();
                                    if (isValidAddress(address)) {
                                        if (null == localAddress) {
                                            localAddress = address;
                                        }
                                        //check preferredNetworks
                                        if (preferredNetworks.length > 0) {
                                            String ip = address.getHostAddress();
                                            for (String regex : preferredNetworks) {
                                                if (StringUtils.isBlank(regex)) {
                                                    continue;
                                                }
                                                if (ip.matches(regex) || ip.startsWith(regex)) {
                                                    return address;
                                                }
                                            }
                                        } else {
                                            return address;
                                        }
                                    }
                                } catch (Throwable e) {
                                    LOGGER.warn("Failed to retrieving ip address, {}", e.getMessage(), e);
                                }
                            }
                        }
                    } catch (Throwable e) {
                        LOGGER.warn("Failed to retrieving ip address, {}", e.getMessage(), e);
                    }
                }
            }
        } catch (Throwable e) {
            LOGGER.warn("Failed to retrieving ip address, {}", e.getMessage(), e);
        }
        if (localAddress == null) {
            LOGGER.error("Could not get local host ip address, will use 127.0.0.1 instead.");
        } else {
            LOGGER.error("Could not match ip by preferredNetworks:{}, will use default first ip {} instead.", Arrays.toString(preferredNetworks), localAddress.getHostAddress());
        }
        return localAddress;
    }

    /**
     * Valid address.
     *
     * @param address the address
     */
    public static void validAddress(InetSocketAddress address) {
        if (address.getHostName() == null || 0 == address.getPort()) {
            throw new IllegalArgumentException("invalid address:" + address);
        }
    }

    /**
     * is valid address
     *
     * @param address
     * @return true if the given address is valid
     */
    private static boolean isValidAddress(InetAddress address) {
        if (address == null || address.isLoopbackAddress()) {
            return false;
        }
        String hostAddress = address.getHostAddress();
        if (address instanceof Inet6Address) {
            if (!PREFER_IPV6_ADDRESSES) {
                return false;
            }
            if (address.isAnyLocalAddress() // filter ::/128
                    || address.isLinkLocalAddress() //filter fe80::/10
                    || address.isSiteLocalAddress()// filter fec0::/10
                    || isUniqueLocalAddress(address)) //filter fd00::/8
            {
                return false;
            }
            return isValidIPv6(hostAddress);
        }
        return !FORBIDDEN_HOSTS.contains(hostAddress) && isValidIPv4(hostAddress);
    }

    /**
     * is valid IP
     *
     * @param ip
     * @param validLocalAndAny Are 127.0.0.1 and 0.0.0.0 valid IPs?
     * @return true if the given IP is valid
     */
    public static boolean isValidIp(String ip, boolean validLocalAndAny) {
        if (ip == null) {
            return false;
        }
        ip = convertIpIfNecessary(ip);
        if (validLocalAndAny) {
            return isValidIPv4(ip) || isValidIPv6(ip);
        } else {
            return !FORBIDDEN_HOSTS.contains(ip) && (isValidIPv4(ip) || isValidIPv6(ip));
        }
    }

    /**
     * convert ip if necessary
     *
     * @param ip
     * @return java.lang.String
     */
    private static String convertIpIfNecessary(String ip) {
        if (isValidIPv4(ip) || isValidIPv6(ip)) {
            return ip;
        } else {
            try {
                return InetAddress.getByName(ip).getHostAddress();
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static boolean isValidIPv4(String ip) {
        return NetAddressValidatorUtil.isIPv4Address(ip);
    }

    public static boolean isValidIPv6(String ip) {
        return NetAddressValidatorUtil.isIPv6Address(ip);
    }

    private static boolean isUniqueLocalAddress(InetAddress address) {
        byte[] ip = address.getAddress();
        return (ip[0] & 0xff) == 0xfd;
    }

    private static String removeBrackets(String str) {
        if (StringUtils.isBlank(str)) {
            return "";
        }
        return str.replaceAll("[\\[\\]]", "");
    }
}
