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

import java.util.ArrayList;
import java.util.List;

import static io.seata.core.constants.DefaultValues.FIRST_PAGE_INDEX;

/**
 * @author wang.liang
 */
public abstract class AbstractQuerier<T> implements Querier<T>, Pageable {

    // sort fields
    protected GlobalTableField sortField;
    protected SortOrder sortOrder;

    // page fields
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
        int toIndex = fromIndex + getPageSize();

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

    /**
     * Compare to
     *
     * @param a the object a
     * @param b the object b
     * @return a negative integer. 0: equals ; -1: a < b ; 1: a > b
     */
    protected int compareTo(Comparable a, Comparable b) {
        int ret;
        if (a == null) {
            if (b == null) {
                return 0;
            } else {
                ret = -1;
            }
        } else {
            if (b == null) {
                ret = 1;
            } else {
                ret = a.compareTo(b);
                if (ret == 0) {
                    return 0;
                }
            }
        }

        if (sortOrder == SortOrder.DESC) {
            if (ret > 0) {
                return -1;
            } else {
                return 1;
            }
        }

        return ret;
    }


    public GlobalTableField getSortField() {
        return sortField;
    }

    public void setSortField(GlobalTableField sortField) {
        this.sortField = sortField;
    }

    public SortOrder getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(SortOrder sortOrder) {
        this.sortOrder = sortOrder;
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
