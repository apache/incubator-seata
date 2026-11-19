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

/**
 * @author jingliu_xiong@foxmail.com
 */
public class TimeCountHelper {

    private long startTime = 0;
    private long totalTime = 0;

    public void startTimeCount() {
        startTime = System.currentTimeMillis();
        totalTime = 0;
    }

    public void pauseTimeCount() {
        long tempTime = System.currentTimeMillis();
        totalTime +=  tempTime - startTime;
        startTime = tempTime;
    }

    /**
     *
     * @return mills
     */
    public long stopTimeCount() {
        long tempTime = System.currentTimeMillis();
        totalTime +=  tempTime - startTime;
        return totalTime;
    }
}
