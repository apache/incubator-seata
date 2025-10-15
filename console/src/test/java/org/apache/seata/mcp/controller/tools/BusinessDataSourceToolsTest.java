package org.apache.seata.mcp.controller.tools;

import org.apache.seata.mcp.entity.pojo.BusinessDataSourcesProperties;
import org.apache.seata.mcp.service.BusinessDataSourceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

public class BusinessDataSourceToolsTest {

    private BusinessDataSourceTools tools;
    private BusinessDataSourceService dataSourceService;

    @BeforeEach
    void setUp() throws Exception {
        tools = new BusinessDataSourceTools();
        dataSourceService = mock(BusinessDataSourceService.class);
        java.lang.reflect.Field f = BusinessDataSourceTools.class.getDeclaredField("dataSourceService");
        f.setAccessible(true);
        f.set(tools, dataSourceService);
    }

    @Test
    void testGetResourceIds() {
        Map<String, String> mockMap = new HashMap<>();
        mockMap.put("name1", "res1");
        try (MockedStatic<BusinessDataSourcesProperties> mocked = mockStatic(BusinessDataSourcesProperties.class)) {
            mocked.when(BusinessDataSourcesProperties::getDataSourcesNamesAndResourceIds)
                    .thenReturn(mockMap);
            Map<String, String> res = tools.getResourceIds();
            assertEquals(mockMap, res);
        }
    }

    @Test
    void testGetTableNames() {
        when(dataSourceService.getTableNamesBySchema(anyString())).thenReturn(Arrays.asList("t1", "t2"));
        List<String> res = tools.getTableNames("jdbc://test");
        assertEquals(Arrays.asList("t1", "t2"), res);
    }

    @Test
    void testGetTableSchema() {
        List<Map<String, Object>> expected = Collections.singletonList(Collections.singletonMap("k", "v"));
        when(dataSourceService.getTableSchemaByTableName("jdbc://test", "tbl")).thenReturn(expected);
        List<Map<String, Object>> res = tools.getTableSchema("tbl", "jdbc://test");
        assertEquals(expected, res);
    }

    @Test
    void testRunSql() {
        List<Map<String, Object>> base = new ArrayList<>();
        base.add(Collections.singletonMap("a", 1));
        when(dataSourceService.runSql("select 1", "jdbc://test")).thenReturn(base);
        List<Map<String, Object>> res = tools.runSql("select 1", "jdbc://test");
        assertEquals(2, res.size());
        assertEquals(1, res.get(0).get("a"));
    }
}
