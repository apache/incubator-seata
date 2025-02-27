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
package org.apache.seata.server.limit.ratelimit;

import org.apache.seata.common.util.UUIDGenerator;

/**
 * The type Rate limit info.
 */
public class RateLimitInfo {

    /**
     * The constant ROLE_TC.
     */
    public static final String GLOBAL_BEGIN_FAILED = "globalBeginFailed";

    /**
     * The Trace id.
     */
    private String traceId;

    /**
     * The Limit type (like GlobalBeginFailed).
     */
    private String limitType;

    /**
     * The Application id.
     */
    private String applicationId;

    /**
     * The Client id.
     */
    private String clientId;

    /**
     * The Server ip address and port.
     */
    private String serverIpAddressAndPort;

    private RateLimitInfo() {
    }

    public static RateLimitInfo generateRateLimitInfo(String applicationId, String type,
                                                      String clientId, String serverIpAddressAndPort) {
        RateLimitInfo rateLimitInfo = new RateLimitInfo();
        rateLimitInfo.setTraceId(String.valueOf(UUIDGenerator.generateUUID()));
        rateLimitInfo.setLimitType(type);
        rateLimitInfo.setApplicationId(applicationId);
        rateLimitInfo.setClientId(clientId);
        rateLimitInfo.setServerIpAddressAndPort(serverIpAddressAndPort);
        return rateLimitInfo;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getLimitType() {
        return limitType;
    }

    public void setLimitType(String limitType) {
        this.limitType = limitType;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getServerIpAddressAndPort() {
        return serverIpAddressAndPort;
    }

    public void setServerIpAddressAndPort(String serverIpAddressAndPort) {
        this.serverIpAddressAndPort = serverIpAddressAndPort;
    }

    @Override
    public String toString() {
        return "RateLimitInfo{" +
                "traceId='" + traceId + '\'' +
                ", limitType='" + limitType + '\'' +
                ", applicationId='" + applicationId + '\'' +
                ", clientId='" + clientId + '\'' +
                ", serverIpAddressAndPort='" + serverIpAddressAndPort + '\'' +
                '}';
    }
}
