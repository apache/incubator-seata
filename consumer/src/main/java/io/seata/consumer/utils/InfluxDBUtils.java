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
package io.seata.consumer.utils;


import io.seata.common.ConfigurationKeys;
import io.seata.common.util.StringUtils;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Point;

import java.util.Map;
import java.util.concurrent.TimeUnit;


public class InfluxDBUtils {
    private static final Configuration CONFIGURATION = ConfigurationFactory.getInstance();
    private static InfluxDB influxDB;
    private static String username;
    private static String password;
    private static String openurl;
    private static String database;

    private static InfluxDB influxDBBuild() {
        try {
            if (influxDB == null) {
                updateBasicParameters();
                if (openurl != null) {
                    influxDB = InfluxDBFactory.connect(openurl, username, password);
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return influxDB;
    }

    private static void updateBasicParameters() {
        username = CONFIGURATION.getConfig(ConfigurationKeys.STORE_INFLUXDB_USERNAME);
        if (StringUtils.isBlank(username)) {
            username = "admin";
        }
        password = CONFIGURATION.getConfig(ConfigurationKeys.STORE_INFLUXDB_PASSWORD);
        if (StringUtils.isBlank(password)) {
            password = "admin";
        }
        openurl = CONFIGURATION.getConfig(ConfigurationKeys.STORE_INFLUXDB_OPENURL);
        if (StringUtils.isBlank(openurl)) {
            openurl = "127.0.0.1";
        }
        database = CONFIGURATION.getConfig(ConfigurationKeys.STORE_INFLUXDB_DATABASE);
        if (StringUtils.isBlank(database)) {
            database = "seata";
        }
    }

    public static void insert(String measurement, Map<String, String> tags, Map<String, Object> fields, Long timestamp) {
        Point.Builder builder = Point.measurement(measurement);
        builder.tag(tags);
        builder.fields(fields);
        builder.time(timestamp, TimeUnit.MILLISECONDS);
        influxDBBuild().write(database, "", builder.build());
    }

}
