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
    void build_PageSizeDivisibleByListSize_CorrectPagination() {
        List<Long> list = new ArrayList<>();
        for (long i = 0; i < 100; i++) {
            list.add(i);
        }
        PageResult pageResult = PageResult.build(list, 1, 10);
        assertEquals(10, pageResult.getPages());
        assertEquals(10, pageResult.getData().size());
    }

    @Test
    void build_PageSizeNotDivisibleByListSize_CorrectPagination() {
        List<Long> list = new ArrayList<>();
        for (long i = 0; i < 9; i++) {
            list.add(i);
        }
        PageResult pageResult = PageResult.build(list, 1, 10);
        assertEquals(1, pageResult.getPages());
        assertEquals(9, pageResult.getData().size());
    }

    @Test
    void build_PageNumGreaterThanTotalPages_DefaultToLastPage() {
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
    void failure_InvalidParams_ReturnsFailure() {
        PageResult pageResult = PageResult.failure("400", "error");
        assertEquals("400", pageResult.getCode());
        assertEquals("error", pageResult.getMessage());
    }

    @Test
    void success_NoData_ReturnsEmptySuccess() {
        PageResult pageResult = PageResult.success();
        assertEquals(PageResult.SUCCESS_CODE, pageResult.getCode());
        assertEquals(PageResult.SUCCESS_MSG, pageResult.getMessage());
        assertNull(pageResult.getData());
    }

    @Test
    void success_WithData_ReturnsSuccess() {
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
    void checkPage_PageNumAndPageSizeDefaults_AppliesDefaults() {
        BaseParam param = new BaseParam();
        param.setPageNum(0);
        param.setPageSize(0);
        PageResult.checkPage(param);
        assertEquals(1, param.getPageNum());
        assertEquals(20, param.getPageSize());
    }

    @Test
    void getTotal_SetAndGet_ReturnsCorrectValue() {
        pageResult.setTotal(100);
        assertEquals(100, pageResult.getTotal().intValue());
    }

    @Test
    void getPages_SetAndGet_ReturnsCorrectValue() {
        pageResult.setPages(10);
        assertEquals(10, pageResult.getPages().intValue());
    }

    @Test
    void getPageNum_SetAndGet_ReturnsCorrectValue() {
        pageResult.setPageNum(2);
        assertEquals(2, pageResult.getPageNum().intValue());
    }

    @Test
    void getPageSize_SetAndGet_ReturnsCorrectValue() {
        pageResult.setPageSize(30);
        assertEquals(30, pageResult.getPageSize().intValue());
    }

    @Test
    void getData_SetAndGet_ReturnsCorrectData() {
        List<Long> list = new ArrayList<>();
        for (long i = 0; i < 5; i++) {
            list.add(i);
        }
        pageResult.setData(list);
        assertEquals(list, pageResult.getData());
    }
}
