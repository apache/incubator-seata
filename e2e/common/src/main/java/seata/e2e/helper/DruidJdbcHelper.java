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

package seata.e2e.helper;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.alibaba.druid.pool.DruidDataSourceFactory;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

/**
 * Used to query the data in the database and run sql script.
 *
 * @author jingliu_xiong@foxmail.com
 */
public class DruidJdbcHelper {

    private JdbcTemplate jdbcTemplateObject;
    private DataSource ds;

    public DruidJdbcHelper(Properties pro) throws Exception {
        this.ds = DruidDataSourceFactory.createDataSource(pro);
        this.jdbcTemplateObject = new JdbcTemplate(ds);
    }

    public DruidJdbcHelper(Map map) throws Exception {
        this.ds = DruidDataSourceFactory.createDataSource(map);
        this.jdbcTemplateObject = new JdbcTemplate(ds);
    }


    public int update(String sql) {
        int update = jdbcTemplateObject.update(sql);
        return update;
    }

    /**
     * @param sql
     * @param requiredType class of a wrapper class of a basic type
     * @param <T>
     * @return
     */
    public <T> T queryForOneObject(String sql, Class<T> requiredType) {
        RowMapper<T> rowMapper = new BeanPropertyRowMapper<T>(requiredType);
        T res = this.jdbcTemplateObject.queryForObject(sql, rowMapper);
        return res;
    }

    /**
     * @param sql
     * @param requiredType class of a wrapper class of a basic type
     * @param <T>
     * @return
     */
    public <T> T queryForOneValue(String sql, Class<T> requiredType) {
        T res = this.jdbcTemplateObject.queryForObject(sql, requiredType);
        return res;
    }

    /**
     * @param sql
     * @param requiredType class of a wrapper class of a basic type
     * @param <T>
     * @return
     */
    public <T> List<T> queryForList(String sql, Class<T> requiredType) {
        RowMapper<T> rowMapper = new BeanPropertyRowMapper<T>(requiredType);
        List<T> res = this.jdbcTemplateObject.query(sql, rowMapper);
        return res;
    }


    public void runSqlScript(String file) throws SQLException, IOException {

        Connection connection = ds.getConnection();
        try {
            ScriptRunner runner = new ScriptRunner(connection);
            runner.setErrorLogWriter(null);
            runner.setLogWriter(null);
            runner.runScript(Resources.getResourceAsReader(file));
        } finally {
            connection.close();
        }
    }

    public JdbcTemplate getJdbcTemplateObject() {
        return jdbcTemplateObject;
    }

    public DataSource getDs() {
        return ds;
    }

}