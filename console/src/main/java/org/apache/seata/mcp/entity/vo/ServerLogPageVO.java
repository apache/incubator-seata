/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.mcp.entity.vo;

import org.apache.seata.common.result.Result;

import java.io.Serializable;
import java.util.List;

public class ServerLogPageVO<T> extends Result<T> implements Serializable {
    private static final long serialVersionUID = 7761262662429121287L;

    private Integer pageSize;

    private Integer pageNum;

    private Integer total = 0;

    private Boolean hasMorePages = false;

    private List<T> data;

    public ServerLogPageVO() {}

    public ServerLogPageVO(String code, String message) {
        super(code, message);
    }

    public ServerLogPageVO(List<T> data, Integer total, Boolean hasMorePages, Integer pageNum, Integer pageSize) {
        super(SUCCESS_CODE, SUCCESS_MSG);
        this.total = total;
        this.hasMorePages = hasMorePages;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.data = data;
    }

    public static <T> ServerLogPageVO<T> failure(String code, String msg) {
        return new ServerLogPageVO<>(code, msg);
    }

    public static <T> ServerLogPageVO<T> success() {
        return new ServerLogPageVO<>(SUCCESS_CODE, SUCCESS_MSG);
    }

    public static <T> ServerLogPageVO<T> success(
            List<T> data, Integer total, Boolean hasMorePages, Integer pageNum, Integer pageSize) {
        return new ServerLogPageVO<>(data, total, hasMorePages, pageNum, pageSize);
    }

    public Integer getTotal() {
        return total;
    }

    public void setTotal(Integer total) {
        this.total = total;
    }

    public Boolean getHasMorePages() {
        return hasMorePages;
    }

    public void setHasMorePages(Boolean hasMorePages) {
        this.hasMorePages = hasMorePages;
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
