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
package io.seata.console.result;

import java.io.Serializable;
import java.util.List;

import io.seata.console.param.BaseParam;
/**
 * The page result
 *
 * @author zhongxiang.wang
 * @author miaoxueyu
 * @author doubleDimple
 */
public class PageResult<T> extends Result<T> implements Serializable {
    private static final long serialVersionUID = 7761262662429121287L;

    /**
     * the page size
     */
    private Integer pageSize;
    /**
     * current page number
     */
    private Integer pageNum;
    /**
     * total result number
     */
    private Integer total = 0;
    /**
     * total page number
     */
    private Integer pages = 0;
    /**
     * the data
     */
    private List<T> data;

    public PageResult() {
    }

    public PageResult(String code, String message) {
        super(code, message);
    }

    public PageResult(List<T> data, Integer total, Integer pages, Integer pageNum, Integer pageSize) {
        super(SUCCESS_CODE, SUCCESS_MSG);
        this.total = total;
        this.pages = pages;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.data = data;
    }

    public static <T> PageResult<T> build(List<T> list, Integer pageNum, Integer pageSize) {
        // calculate pages
        int pages = list.size() / pageSize;
        if (list.size() % pageSize != 0) {
            pages++;
        }
        final int offset = pageSize * (pageNum - 1);
        return PageResult.success(
                list.subList(
                        Math.min(offset, list.size()),
                        Math.min(offset + pageSize, list.size())
                ),
                list.size(),
                pages,
                pageNum,
                pageSize
        );
    }

    public PageResult(List<T> data, Integer total, Integer pageNum, Integer pageSize) {
        super(SUCCESS_CODE, SUCCESS_MSG);
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.data = data;

        if (total % pageSize == 0) {
            this.pages = total / pageSize;
        } else {
            this.pages = total / pageSize + 1;
        }
    }

    public static <T> PageResult<T> failure(String code, String msg) {
        return new PageResult<>(code, msg);
    }

    public static <T> PageResult<T> success() {
        return new PageResult<>(SUCCESS_CODE, SUCCESS_MSG);
    }

    public static <T> PageResult<T> success(List<T> data, Integer total, Integer pages, Integer pageNum, Integer pageSize) {
        return new PageResult<>(data, total, pages, pageNum, pageSize);
    }
    public static <T> PageResult<T> success(List<T> data, Integer total, Integer pageNum, Integer pageSize) {
        return new PageResult<>(data, total, pageNum, pageSize);
    }

    public static void checkPage(BaseParam param) {
        if (param.getPageNum() <= 0) {
            param.setPageNum(1);
        }

        if (param.getPageSize() <= 0) {
            param.setPageSize(20);
        }
    }

    public Integer getTotal() {
        return total;
    }

    public void setTotal(Integer total) {
        this.total = total;
    }

    public Integer getPages() {
        return pages;
    }

    public void setPages(Integer pages) {
        this.pages = pages;
    }

    public Integer getPageNum() {
        return pageNum;
    }

    public void setPageNum(Integer pageNum) {
        this.pageNum = pageNum;
    }

    public Integer getCurrPage() {
        return this.pageNum;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public List<T> getData() {
        return data;
    }

    public void setData(List<T> data) {
        this.data = data;
    }
}
