package org.apache.seata.rm.datasource.util;

import com.alibaba.druid.util.MySqlUtils;
import com.alibaba.druid.util.PGUtils;
import org.apache.seata.rm.BaseDataSourceResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.XAConnection;
import javax.transaction.xa.XAException;
import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.List;

import static org.apache.seata.sqlparser.util.JdbcConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class XAUtilsTest {
    private Connection mockConnection;
    private Driver mockDriver;
    private BaseDataSourceResource mockDataSourceResource;

    @BeforeEach
    public void setUp() {
        mockConnection = mock(Connection.class);
        mockDriver = mock(Driver.class);
        mockDataSourceResource = mock(BaseDataSourceResource.class);
        when(mockDataSourceResource.getDriver()).thenReturn(mockDriver);

    }

    @Test
    public void testGetDbType() {
        try (MockedStatic<JdbcUtils> jdbcUtilsMock = Mockito.mockStatic(JdbcUtils.class)) {
            jdbcUtilsMock.when(() -> JdbcUtils.getDbType(anyString()))
                    .thenReturn("mysql");

            String dbType = XAUtils.getDbType("jdbc:mysql://localhost:3306/test", "com.mysql.Driver");
            assertEquals("mysql", dbType);
        }
    }

    @Test
    public void testCreateXAConnectionMySQL() throws SQLException {
        when(mockDataSourceResource.getDbType()).thenReturn(MYSQL);
        XAConnection mockXAConnection = mock(XAConnection.class);

        try (MockedStatic<MySqlUtils> mySqlUtilsMock = Mockito.mockStatic(MySqlUtils.class)) {
            mySqlUtilsMock.when(() -> MySqlUtils.createXAConnection(any(), any()))
                    .thenReturn(mockXAConnection);

            XAConnection result = XAUtils.createXAConnection(mockConnection, mockDataSourceResource);
            assertSame(mockXAConnection, result);
        }
    }

    @Test
    public void testCreateXAConnectionPostgreSQL() throws SQLException,ClassNotFoundException {
        when(mockDataSourceResource.getDbType()).thenReturn(POSTGRESQL);
        XAConnection mockXAConnection = mock(XAConnection.class);

        try (MockedStatic<PGUtils> pgUtilsMock = Mockito.mockStatic(PGUtils.class)) {
            pgUtilsMock.when(() -> PGUtils.createXAConnection(any()))
                    .thenReturn(mockXAConnection);

            XAConnection result = XAUtils.createXAConnection(mockConnection, mockDataSourceResource);
            assertSame(mockXAConnection, result);
        }
    }

    @Test
    public void testCreateXAConnectionOracle() throws SQLException,ClassNotFoundException {
        when(mockDataSourceResource.getDbType()).thenReturn(ORACLE);
// 模拟Oracle T4CConnection
        Class<?> oracleT4CConnection = Class.forName("oracle.jdbc.driver.T4CConnection");
        Connection t4cMockConnection = mock(oracleT4CConnection.asSubclass(Connection.class));
        // 测试T4CConnection路径
        when(t4cMockConnection.getClass()).thenAnswer(inv -> {
            Class<?> mockClass = mock(Class.class);
            when(mockClass.getName()).thenReturn("oracle.jdbc.driver.T4CConnection");
            return mockClass;
        });

        XAConnection result = XAUtils.createXAConnection(t4cMockConnection, mockDataSourceResource);
        assertNotNull(result);
    }

    @Test
    public void testCreateXAConnectionOracleFallback() throws SQLException,ClassNotFoundException {
        when(mockDataSourceResource.getDbType()).thenReturn(ORACLE);
        // 模拟普通的Oracle连接
        Class<?> oracleConnection = Class.forName("oracle.jdbc.OracleConnection");
        Connection oracleMockConnection = mock(oracleConnection.asSubclass(Connection.class));
        // 测试非T4CConnection路径
        when(oracleMockConnection.getClass()).thenAnswer(inv -> {
            Class<?> mockClass = mock(Class.class);
            when(mockClass.getName()).thenReturn("oracle.other.Connection");
            return mockClass;
        });

        XAConnection result = XAUtils.createXAConnection(mockConnection, mockDataSourceResource);
        assertNotNull(result);
    }

    @Test
    public void testCreateXAConnectionMariaDB() throws SQLException,ClassNotFoundException {
        when(mockDataSourceResource.getDbType()).thenReturn(MARIADB);

        // 模拟MariaDB特定的连接类
        Class<?> mariaDbConnectionClass = Class.forName("org.mariadb.jdbc.MariaDbConnection");
        Connection mariaMockConnection = mock(mariaDbConnectionClass.asSubclass(Connection.class));
        try (MockedConstruction<?> ignored = mockConstruction(
                Class.forName("org.mariadb.jdbc.MariaXaConnection").asSubclass(XAConnection.class),
                (mock, context) -> {
                    // 验证构造器参数类型
                    Connection connectionParam = (Connection) context.arguments().get(0);
                    assertSame(mariaMockConnection, connectionParam);
                })) {

            XAConnection result = XAUtils.createXAConnection(mariaMockConnection, mockDataSourceResource);
            assertNotNull(result);
        } catch (ClassNotFoundException e) {
            fail("MariaDB XAConnection class not found in test environment");
        }
    }

    @Test
    public void testCreateXAConnectionKingbase() throws SQLException,ClassNotFoundException {
        when(mockDataSourceResource.getDbType()).thenReturn(KINGBASE);
        // 模拟Kingbase特定的连接类
        Class<?> kingbaseConnectionClass = Class.forName("com.kingbase8.core.BaseConnection");
        Connection kingbaseMockConnection = mock(kingbaseConnectionClass.asSubclass(Connection.class));
        try (MockedConstruction<?> ignored = mockConstruction(
                Class.forName("com.kingbase8.xa.KBXAConnection").asSubclass(XAConnection.class),
                (mock, context) -> {
                    // 验证构造器参数类型
                    Connection connectionParam = (Connection) context.arguments().get(0);
                    assertSame(kingbaseMockConnection, connectionParam);
                })) {

            XAConnection result = XAUtils.createXAConnection(kingbaseMockConnection, mockDataSourceResource);
            assertNotNull(result);
        } catch (ClassNotFoundException e) {
            fail("Kingbase XAConnection class not found in test environment");
        }
    }

    @Test
    public void testCreateXAConnectionDM() throws SQLException,ClassNotFoundException {
        when(mockDataSourceResource.getDbType()).thenReturn(DM);

        // 模拟达梦特定的连接类
        Class<?> dmConnectionClass = Class.forName("dm.jdbc.driver.DmdbConnection");
        Connection dmMockConnection = mock(dmConnectionClass.asSubclass(Connection.class));

        try (MockedConstruction<?> ignored = mockConstruction(
                Class.forName("dm.jdbc.driver.DmdbXAConnection").asSubclass(XAConnection.class),
                (mock, context) -> {
                    // 验证构造器参数类型
                    Connection connectionParam = (Connection) context.arguments().get(0);
                    assertSame(dmMockConnection, connectionParam);
                })) {

            XAConnection result = XAUtils.createXAConnection(dmMockConnection, mockDataSourceResource);
            assertNotNull(result);
        } catch (ClassNotFoundException e) {
            fail("DM XAConnection class not found in test environment");
        }
    }

    @Test
    public void testCreateXAConnectionUnsupportedDB() {
        when(mockDataSourceResource.getDbType()).thenReturn("UNKNOWN_DB");

        SQLException exception = assertThrows(SQLException.class, () ->
                XAUtils.createXAConnection(mockConnection, mockDataSourceResource));

        assertEquals("xa not support dbType: UNKNOWN_DB", exception.getMessage());
    }

    @Test
    public void testCreateXAConnection_ReflectionFailure() throws SQLException {
        when(mockDataSourceResource.getDbType()).thenReturn(ORACLE);
        when(mockConnection.getClass()).thenAnswer(inv -> {
            Class<?> mockClass = mock(Class.class);
            when(mockClass.getName()).thenReturn("oracle.jdbc.driver.T4CConnection");
            return mockClass;
        });

        try (MockedStatic<Class> classMock = mockStatic(Class.class)) {
            classMock.when(() -> Class.forName(anyString()))
                    .thenThrow(new ClassNotFoundException("Test exception"));

            SQLException exception = assertThrows(SQLException.class, () ->
                    XAUtils.createXAConnection(mockConnection, mockDataSourceResource));

            assertEquals("create xaConnection error", exception.getMessage());
            assertInstanceOf(ClassNotFoundException.class, exception.getCause());
        }
    }

    @Test
    public void testCreateXAConnection_ConstructorFailure() throws SQLException {
        when(mockDataSourceResource.getDbType()).thenReturn(ORACLE);
        when(mockConnection.getClass()).thenAnswer(inv -> {
            Class<?> mockClass = mock(Class.class);
            when(mockClass.getName()).thenReturn("oracle.jdbc.driver.T4CConnection");
            return mockClass;
        });

        try (MockedStatic<Class> classMock = mockStatic(Class.class)) {
            // 模拟真实的反射过程
            classMock.when(() -> Class.forName(anyString())).thenCallRealMethod();

            // 使用模拟构造器使其抛出异常
            try (MockedConstruction<?> ignored = mockConstruction(
                    Class.forName("oracle.jdbc.driver.T4CXAConnection").asSubclass(XAConnection.class),
                    (mock, context) -> {
                        throw new XAException("Test XA error");
                    })) {

                XAException exception = assertThrows(XAException.class, () ->
                        XAUtils.createXAConnection(mockConnection, mockDataSourceResource));

                assertEquals("Test XA error", exception.getMessage());
            } catch (ClassNotFoundException e) {
                fail("Oracle XAConnection class not found");
            }
        }
    }

}
