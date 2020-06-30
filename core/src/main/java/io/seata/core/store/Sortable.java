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

/**
 * @author wang.liang
 */
public interface Sortable {

    /**
     * Gets sort field name.
     *
     * @return the sort field name
     */
    String getSortFieldName();

    /**
     * Sets sort field name.
     *
     * @param sortFieldName the sort field name
     */
    void setSortFieldName(String sortFieldName);

    /**
     * Gets sort order.
     *
     * @return the sort order
     */
    SortOrder getSortOrder();

    /**
     * Sets sort order.
     *
     * @param sortOrder the sort order
     */
    void setSortOrder(SortOrder sortOrder);
}
