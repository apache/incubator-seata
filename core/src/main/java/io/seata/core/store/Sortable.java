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
package io.seata.core.store;

import org.apache.commons.lang.ArrayUtils;

/**
 * @author wang.liang
 */
public interface Sortable {

    /**
     * Gets sort params.
     *
     * @return the sort params
     */
    SortParam[] getSortParams();

    /**
     * Sets sort params.
     *
     * @param sortParams the sort params
     */
    void setSortParams(SortParam... sortParams);

    /**
     * Sets sort fields, and all fields use SortOrder.ASC
     *
     * @param sortFieldNames the sort field names
     */
    default void setSortFieldNames(String... sortFieldNames) {
        if (sortFieldNames.length == 0) {
            return;
        }
        SortParam[] sortParams = new SortParam[sortFieldNames.length];
        for (int i = 0, l = sortFieldNames.length; i < l; ++i) {
            sortParams[i] = new SortParam(sortFieldNames[i]);
        }
        this.setSortParams(sortParams);
    }

    /**
     * Is need sort boolean
     *
     * @return the boolean
     */
    default boolean isNeedSort() {
        return ArrayUtils.isNotEmpty(getSortParams());
    }
}
