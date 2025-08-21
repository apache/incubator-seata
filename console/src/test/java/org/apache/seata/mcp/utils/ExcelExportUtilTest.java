package org.apache.seata.mcp.utils;

import org.junit.Assert;
import org.junit.Test;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.apache.seata.mcp.utils.ExcelExportUtil.exportToExcelBase64;

public class ExcelExportUtilTest {

    @Test
    public void testExportToBase64Excel() {
        try {
            // Sample data for demonstration
            List<Map<String, Object>> sampleData = createSampleData();
            // Export to Excel base64
            Assert.assertNotNull(exportToExcelBase64(sampleData, "Employee Data"));
        } catch (Exception e) {
            System.err.println("Error exporting to Excel base64: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Create sample data for demonstration
     *
     * @return List of sample data maps
     */
    private static List<Map<String, Object>> createSampleData() {
        List<Map<String, Object>> dataList = new java.util.ArrayList<>();

        // Sample employee data
        Map<String, Object> employee1 = new java.util.HashMap<>();
        employee1.put("ID", 1);
        employee1.put("Name", "John Doe");
        employee1.put("Age", 30);
        employee1.put("Salary", 75000.50);
        employee1.put("Department", "Engineering");
        employee1.put("Active", true);
        employee1.put("Hire Date", new Date());

        Map<String, Object> employee2 = new java.util.HashMap<>();
        employee2.put("ID", 2);
        employee2.put("Name", "Jane Smith");
        employee2.put("Age", 28);
        employee2.put("Salary", 68000.75);
        employee2.put("Department", "Marketing");
        employee2.put("Active", true);
        employee2.put("Hire Date", new Date(System.currentTimeMillis() - 86400000L)); // Yesterday

        Map<String, Object> employee3 = new java.util.HashMap<>();
        employee3.put("ID", 3);
        employee3.put("Name", "Bob Johnson");
        employee3.put("Age", 35);
        employee3.put("Salary", 82000.00);
        employee3.put("Department", "Engineering");
        employee3.put("Active", false);
        employee3.put("Hire Date", new Date(System.currentTimeMillis() - 172800000L)); // 2 days ago

        dataList.add(employee1);
        dataList.add(employee2);
        dataList.add(employee3);

        return dataList;
    }
}
