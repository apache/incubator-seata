package io.seata.server.storage.tsdb.influxdb;

import io.seata.common.util.StringUtils;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class InfluxDBUtils {
    private static InfluxDB influxDB;
    private static String username;
    private static String password;
    private static String openurl;
    private static String database;

    private static final Configuration CONFIGURATION = ConfigurationFactory.getInstance();

    public static InfluxDB influxDBBuild() {
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

    public static void batchInsert(String measurement, List<Map<String, String>> tagList, List<Map<String, Object>> fieldList, List<Long> timestamps) {
        if (tagList.size() != fieldList.size() && fieldList.size() != timestamps.size()) {
            throw new IllegalArgumentException();
        }
        BatchPoints batchPoints = BatchPoints.database(database)
                .consistency(InfluxDB.ConsistencyLevel.ALL).build();
        for (int i = 0; i < tagList.size(); i++) {
            Point point = Point
                    .measurement(measurement)
                    .tag(tagList.get(i))
                    .fields(fieldList.get(i))
                    .time(timestamps.get(i), TimeUnit.MILLISECONDS)
                    .build();
            batchPoints.point(point);
        }
        influxDBBuild().write(batchPoints);
    }

}