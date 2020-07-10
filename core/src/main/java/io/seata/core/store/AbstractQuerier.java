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

import io.seata.common.util.ComparableUtils;

import java.util.ArrayList;
import java.util.List;

import static io.seata.core.constants.DefaultValues.FIRST_PAGE_INDEX;

/**
 * @author wang.liang
 */
public abstract class AbstractQuerier<T> implements Querier<T>, Sortable, Pageable {

    // sort params
    protected SortParam[] sortParams;

    // page params
    protected int pageIndex = FIRST_PAGE_INDEX;
    protected int pageSize = 0;

    /**
     * Do paging.
     *
     * @param list the list
     * @return the list after paging
     */
    @Override
    public <D extends T> List<D> doPaging(List<D> list) {
        if (list == null) {
            return new ArrayList<>();
        }

        if (list.isEmpty() || getPageSize() <= 0) {
            return list;
        }

        int fromIndex = this.getFromIndex();
        int toIndex = this.getToIndex(fromIndex);

        if (fromIndex >= list.size()) {
            return new ArrayList<>();
        }

        if (toIndex > list.size()) {
            toIndex = list.size();
        }

        return list.subList(fromIndex, toIndex);
    }

    public int getFromIndex() {
        return (pageIndex - FIRST_PAGE_INDEX) * pageSize;
    }

    public int getToIndex(int fromIndex) {
        return fromIndex + pageSize;
    }

    /**
     * Compare fieldValueA and fieldValueB.
     *
     * @param fieldValueA the field value a
     * @param fieldValueB the field value b
     * @param sortOrder   the sort order
     * @return 0: equals    -1: a < b    1: a > b
     */
    protected int compare(Comparable fieldValueA, Comparable fieldValueB, SortOrder sortOrder) {
        int ret = ComparableUtils.compare(fieldValueA, fieldValueB);
        if (ret == 0) {
            return ret;
        }

        if (sortOrder == SortOrder.DESC) {
            return ret > 0 ? -1 : 1; // -1 * ret
        }

        return ret;
    }

    @Override
    public SortParam[] getSortParams() {
        return sortParams;
    }

    @Override
    public void setSortParams(SortParam... sortParams) {
        this.sortParams = sortParams;
    }

    @Override
    public int getPageIndex() {
        return pageIndex;
    }

    @Override
    public void setPageIndex(int pageIndex) {
        this.pageIndex = pageIndex;
    }

    @Override
    public int getPageSize() {
        return pageSize;
    }

    @Override
    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
}
