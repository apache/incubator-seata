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

import seata.e2e.helper.*;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;

/**
 * Return some tools commonly used in testing
 */
public interface SeataTestHelperFactory {

    /**
     *
     * @param map The configuration required to initialize the durid connection pool
     * @return
     */
    public DruidJdbcHelper druidJdbcQuery(Map map);

    /**
     *
     * @param pro The configuration required to initialize the durid connection pool
     * @return
     */
    public DruidJdbcHelper druidJdbcQuery(Properties pro);

    /**
     *
     * @param sender Tasks to be performed
     * @param times Number of times to execute
     * @param interval Interval between each execution
     * @return
     */
    public TimesTask timseTask(Callable<?> sender, int times, int interval);

    /**
     *
     * @param sender Tasks to be performed
     * @param clientTotal Number of times to execute
     * @param threadTotal Maximum number of execution threads
     * @return
     */
    public PressureTask pressureController(Callable<?> sender, int clientTotal, int threadTotal);

    /**
     *
     * @return
     */
    public TimeCountHelper timeCountHelper();

    /**
     *
     * @param interval Interval between each execution
     * @param sender Tasks to be performed
     * @return
     */
    public CronTask cronTask(int interval, Callable<?> sender);
}
