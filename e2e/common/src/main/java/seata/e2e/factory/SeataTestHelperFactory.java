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
package seata.e2e.factory;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;


import com.alibaba.druid.pool.DruidDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import seata.e2e.helper.CronTask;
import seata.e2e.helper.DruidJdbcHelper;
import seata.e2e.helper.PressureTask;
import seata.e2e.helper.TimeCountHelper;
import seata.e2e.helper.TimesTask;

/**
 * Return some tools commonly used in testing
 *
 * @author jingliu_xiong@foxmail.com
 */
public interface SeataTestHelperFactory {

    /**
     * DruidJdbcHelper encapsulates {@link DruidDataSource} connection pool and {@link JdbcTemplate}. Allow developers
     * to directly use the orm method to query data during testing. It also allows simple update statement execution and
     * sql script execution. If you need to perform comprehensive database operations, it is recommended to directly
     * use frameworks such as JPA.
     * @param map The configuration required to initialize the druid connection pool
     * @return
     */
    public DruidJdbcHelper druidJdbcQuery(Map map);

    /**
     * DruidJdbcHelper encapsulates {@link DruidDataSource} connection pool and {@link JdbcTemplate}. Allow developers
     * to directly use the orm method to query data during testing. It also allows simple update statement execution and
     * sql script execution. If you need to perform comprehensive database operations, it is recommended to directly
     * use frameworks such as JPA.
     * @param prop The configuration required to initialize the druid connection pool
     * @return
     */
    public DruidJdbcHelper druidJdbcQuery(Properties prop);

    /**
     * TimesTask executes tasks according to the execution times and execution time interval set by the developer.
     * @param sender Tasks to be performed
     * @param times Number of times to execute
     * @param interval Interval between each execution
     * @return
     */
    public TimesTask timseTask(Callable<?> sender, int times, int interval);

    /**
     *  PressureTask executes pressure tasks according to the number of threads and total execution times set
     *  by the developer, and the developer can pass in related functions for the callback processing of each test.
     * @param sender Tasks to be performed
     * @param clientTotal Number of times to execute
     * @param threadTotal Maximum number of execution threads
     * @return
     */
    public PressureTask pressureController(Callable<?> sender, int clientTotal, int threadTotal);

    /**
     * TimeCountHelper is a time count tool that supports time pausing and settling the total time spent.
     * @return
     */
    public TimeCountHelper timeCountHelper();

    /**
     * CronTask executes tasks according to the time interval set by the developer, it will not stop until it is
     * manually stopped by code.
     * @param interval Interval between each execution
     * @param sender Tasks to be performed
     * @return
     */
    public CronTask cronTask(int interval, Callable<?> sender);
}
