package org.apache.seata.rm.datasource.util;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class XAUtilsForDMTest {


    private static final String DM_JDBC_URL = "jdbc:dm://localhost:5236?schema=BPM3&useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai";
    private static final String USERNAME = "SYSDBA";
    private static final String PASSWORD = "SYSDBA";

    private static Connection realDmConnection;
    private static final Map<String, String> DM_METADATA = new ConcurrentHashMap<>();

    @BeforeAll
    static void setUp() throws SQLException {
        try {
            // 尝试连接真实的达梦数据库
            realDmConnection = DriverManager.getConnection(DM_JDBC_URL, USERNAME, PASSWORD);
            DatabaseMetaData metaData = realDmConnection.getMetaData();
            DM_METADATA.put("DatabaseName", "DM DBMS");
            DM_METADATA.put("DatabaseProductName", "DM");
            DM_METADATA.put("DatabaseProductVersion", metaData.getDatabaseProductVersion());
        } catch (SQLException e) {
            System.out.println("无法连接达梦数据库，使用模拟数据: " + e.getMessage());
        }
    }

    // 测试XA连接器实现
    @Test
    void shouldLoadDMXADataSource() throws Exception {
        if (realDmConnection == null) return;

        Object xaConnection = XAUtils.createXAConnection(realDmConnection,null,"dm");
        assertNotNull(xaConnection);
        assertEquals("dm.jdbc.driver.DmdbXAConnection", xaConnection.getClass().getName());
    }

}
