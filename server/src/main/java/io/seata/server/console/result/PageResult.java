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
package io.seata.server.console.result;

import io.seata.common.exception.FrameworkErrorCode;

import java.util.List;

/**
 * The page result
 * @author: zhongxiang.wang
 */
public class PageResult<T> extends Result<T> {
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

    public PageResult(String errCode, String code) {
    }

    public PageResult(List<T> data, Integer total, Integer pages, Integer pageNum, Integer pageSize) {
        super(SUCCESS_CODE, SUCCESS_MSG, data);
        this.total = total;
        this.pages = pages;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
    }

    public static <T> PageResult<T> failure(String code, String msg) {
        return new PageResult<>(code, msg);
    }

    public static <T> PageResult<T> failure(FrameworkErrorCode errorCode) {
        return new PageResult(errorCode.getErrCode(), errorCode.getErrCode());
    }

    public static <T> PageResult<T> success() {
        return new PageResult<>(SUCCESS_CODE, SUCCESS_MSG);
    }

    public static <T> PageResult<T> create() {
        return new PageResult<>(SUCCESS_CODE, SUCCESS_MSG);
    }

    public static <T> PageResult<T> success(List<T> data, Integer total, Integer pages, Integer pageNum, Integer pageSize) {
        return new PageResult<>(data, total, pages, pageNum, pageSize);
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
}
