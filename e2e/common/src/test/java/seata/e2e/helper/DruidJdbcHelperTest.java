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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Properties;

/**
 * @author jingliu_xiong@foxmail.com
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DruidJdbcHelperTest {

    private DruidJdbcHelper druidJdbcHelper;

    @BeforeAll
    public void init() throws Exception {
        Properties properties = new Properties();
        InputStream is = DruidJdbcHelper.class.getClassLoader().getResourceAsStream("druid.properties");
        properties.load(is);
        druidJdbcHelper = new DruidJdbcHelper(properties);
    }

    @Test
    public void testRunSqlScript() throws IOException, SQLException {
        druidJdbcHelper.runSqlScript("storage.sql");
    }
}