/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
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

package com.alibaba.fescar.rm.datasource.sql.druid;

import com.alibaba.fescar.rm.datasource.sql.SQLRecognizer;

public abstract class BaseRecognizer implements SQLRecognizer {

    public static class VMarker {
        @Override
        public String toString() {
            return "?";
        }

    }

    protected String originalSQL;

    public BaseRecognizer(String originalSQL) {
        this.originalSQL = originalSQL;

    }

    @Override
    public String getOriginalSQL() {
        return originalSQL;
    }
}
