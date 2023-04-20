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
package io.seata.sqlparser;

import java.util.ArrayList;
import java.util.List;

/**
 * The interface Where recognizer.
 *
 * @author renliangyu857
 */
public interface JoinRecognizer {
    /**
     * Gets join condition.
     * @param parametersHolder the parameters holder
     * @param paramAppenderList the param appender list
     * @return the join condition
     */
    String getJoinCondition(ParametersHolder parametersHolder, ArrayList<List<Object>> paramAppenderList);
}
