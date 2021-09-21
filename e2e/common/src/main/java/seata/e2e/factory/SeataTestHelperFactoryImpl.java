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
 * Integrate some helpers commonly used in scene test.
 */
public class SeataTestHelperFactoryImpl implements SeataTestHelperFactory{

    @Override
    public DruidJdbcHelper druidJdbcQuery(Map map) {
        DruidJdbcHelper druidJdbcHelper = null;
        try {
            druidJdbcHelper = new DruidJdbcHelper(map);
            return druidJdbcHelper;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return druidJdbcHelper;
    }

    @Override
    public DruidJdbcHelper druidJdbcQuery(Properties pro) {
        DruidJdbcHelper druidJdbcHelper = null;
        try {
            druidJdbcHelper = new DruidJdbcHelper(pro);
            return druidJdbcHelper;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return druidJdbcHelper;
    }


    @Override
    public TimesTask timseTask(Callable<?> sender, int times, int interval){

        TimesTask timesTask = new TimesTask(sender, times, interval);
        return timesTask;
    }

    @Override
    public PressureTask pressureController(Callable<?> sender, int clientTotal, int threadTotal){

        PressureTask pressureTask = new PressureTask(sender, clientTotal, threadTotal);
        return pressureTask;

    }

    @Override
    public TimeCountHelper timeCountHelper() {
        return new TimeCountHelper();
    }

    @Override
    public CronTask cronTask(int interval, Callable<?> sender) {
        return new CronTask(interval, sender);
    }


}