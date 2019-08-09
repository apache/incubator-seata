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
package io.seata.discovery.registry;


import java.util.HashMap;
import java.util.Map;

public class RegistryUrl {

    private final String host;

    private final int port;

    private final String schema;

    private final HashMap<String, String> parameters;

    public RegistryUrl() {
        this(null, null, 0, null);
    }

    public RegistryUrl(String host, int port) {
        this(null, host, port, null);
    }

    public RegistryUrl(String schema, String host, int port, Map<String, String> parameters) {
        this.schema = schema;
        this.host = host;
        this.port = port;
        if (parameters == null) {
            this.parameters = new HashMap<>();
        } else {
            this.parameters = new HashMap<>(parameters);
        }
    }

    public static RegistryUrl valueOf(String url) {
        if (url == null || url.trim().length() == 0) {
            throw new IllegalArgumentException("url is null or empty string");
        }
        int port;
        String host;
        String schema;
        String param;
        int idxOfSchema = url.indexOf("://");
        int idxOfPort;
        int idxOfPara = url.indexOf("?");
        int idxOfHost;
        Map<String, String> paramters = null;
        if (idxOfSchema < 0) {
            schema = null;
            idxOfHost = 0;
            idxOfPort = url.indexOf(":");
        } else {
            schema = url.substring(0, idxOfSchema);
            idxOfHost = idxOfSchema + 3;
            idxOfPort = url.indexOf(":", idxOfSchema + 3);
        }
        if (idxOfPara > 0) {
            param = url.substring(idxOfPara + 1);
            paramters = parseParamter(param);
        }
        if (idxOfPort < 0) {
            port = 0;
            if (idxOfPara < 0) {
                host = url.substring(idxOfHost);
            } else {
                host = url.substring(idxOfHost, idxOfPara);
            }
        } else if (idxOfPort > 0 && idxOfPara > 0) {
            port = Integer.valueOf(url.substring(idxOfPort + 1, idxOfPara));
            host = url.substring(idxOfHost, idxOfPort);
        } else {
            port = Integer.valueOf(url.substring(idxOfPort + 1));
            host = url.substring(idxOfHost, idxOfPort);
        }
        return new RegistryUrl(schema, host, port, paramters);
    }

    public static Map<String, String> parseParamter(String paraStr) {

        HashMap<String, String> parameters = new HashMap<>();

        String[] pair = paraStr.split("&");
        for (int i = 0; i < pair.length; i++) {
            String str = pair[i];
            int idxOfeql = str.indexOf("=");
            if (idxOfeql > 0) {
                String val = null;
                String key = str.substring(0, idxOfeql);
                if ((idxOfeql + 1) < str.length()) {
                    val = str.substring(idxOfeql + 1);
                }
                parameters.put(key, val);
            }
        }

        return parameters;
    }


    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getSchema() {
        return schema;
    }

    public String getStringParam(String key) {
        return this.parameters.get(key);
    }

    @Override
    public String toString() {
        return "RegistryUrl{" +
                "host='" + host + '\'' +
                ", port=" + port +
                ", schema='" + schema + '\'' +
                ", parameters=" + parameters +
                '}';
    }
}
