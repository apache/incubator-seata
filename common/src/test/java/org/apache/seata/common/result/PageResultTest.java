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
package org.apache.seata.common.result;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class PageResultTest {
    @InjectMocks
    private PageResult pageResult;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void buildPageSizeDivisibleByListSize() {
        List<Long> list = new ArrayList<>();
        for (long i = 0; i < 100; i++) {
            list.add(i);
        }
        PageResult pageResult = PageResult.build(list, 1, 10);
        assertEquals(10, pageResult.getPages());
        assertEquals(10, pageResult.getData().size());
    }

    @Test
    void buildPageSizeNotDivisibleByListSize() {
        List<Long> list = new ArrayList<>();
        for (long i = 0; i < 9; i++) {
            list.add(i);
        }
        PageResult pageResult = PageResult.build(list, 1, 10);
        assertEquals(1, pageResult.getPages());
        assertEquals(9, pageResult.getData().size());
    }

    @Test
    void buildPageNumGreaterThanTotalPages() {
        List<Long> list = new ArrayList<>();
        for (long i = 0; i < 5; i++) {
            list.add(i);
        }
        PageResult pageResult = PageResult.build(list, 10, 2);
        assertEquals(10, pageResult.getPageNum().intValue());
        assertEquals(3, pageResult.getPages().intValue());
        assertEquals(0, pageResult.getData().size());
    }

    @Test
    void failureInvalidParams() {
        PageResult pageResult = PageResult.failure("400", "error");
        assertEquals("400", pageResult.getCode());
        assertEquals("error", pageResult.getMessage());
    }

    @Test
    void successNoData() {
        PageResult pageResult = PageResult.success();
        assertEquals(PageResult.SUCCESS_CODE, pageResult.getCode());
        assertEquals(PageResult.SUCCESS_MSG, pageResult.getMessage());
        assertNull(pageResult.getData());
    }

    @Test
    void successWithData() {
        List<Long> list = new ArrayList<>();
        for (long i = 0; i < 5; i++) {
            list.add(i);
        }
        PageResult pageResult = PageResult.success(list, 5, 1, 5);
        assertEquals(PageResult.SUCCESS_CODE, pageResult.getCode());
        assertEquals(PageResult.SUCCESS_MSG, pageResult.getMessage());
        assertEquals(5, pageResult.getTotal().intValue());
        assertEquals(1, pageResult.getPageNum().intValue());
        assertEquals(5, pageResult.getPageSize().intValue());
        assertEquals(1, pageResult.getPages().intValue());
        assertEquals(list, pageResult.getData());
    }

    @Test
    void checkPageNumAndPageSizeDefault() {
        BaseParam param = new BaseParam();
        param.setPageNum(0);
        param.setPageSize(0);
        param.setTimeStart(1L);
        param.setTimeEnd(2L);
        PageResult.checkPage(param);
        assertEquals(1, param.getPageNum());
        assertEquals(20, param.getPageSize());
        assertEquals(1L, param.getTimeStart());
        assertEquals(2L, param.getTimeEnd());
        assertEquals("BaseParam{pageNum=1, pageSize=20, timeStart=1, timeEnd=2}", param.toString());
    }

    @Test
    void getTotalSetAndGet() {
        pageResult.setTotal(100);
        assertEquals(100, pageResult.getTotal().intValue());
    }

    @Test
    void getPagesSetAndGet() {
        pageResult.setPages(10);
        assertEquals(10, pageResult.getPages().intValue());
    }

    @Test
    void getPageNumSetAndGet() {
        pageResult.setPageNum(2);
        assertEquals(2, pageResult.getPageNum().intValue());
    }

    @Test
    void getPageSizeSetAndGet() {
        pageResult.setPageSize(30);
        assertEquals(30, pageResult.getPageSize().intValue());
    }

    @Test
    void getDataSetAndGet() {
        List<Long> list = new ArrayList<>();
        for (long i = 0; i < 5; i++) {
            list.add(i);
        }
        pageResult.setData(list);
        assertEquals(list, pageResult.getData());
    }
}
