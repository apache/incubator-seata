package org.apache.seata.mcp.utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.Date;

/**
 * Utility class for converting List<Map<String, Object>> to Excel base64 string using Apache POI
 * Compatible with older POI versions to avoid Commons IO dependency issues
 *
 */
public class ExcelExportUtil {

    private static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    /**
     * Export List<Map<String, Object>> to Excel file (.xls) with custom headers
     *
     * @param data  List of data maps
     * @param headers   Custom headers (order will be preserved). If null or empty, keys will be auto-extracted.
     * @param sheetName Name of the sheet
     * @param filePath  Output file path (e.g. "C:/Users/test/Desktop/data.xls")
     * @throws IOException if generation fails
     */
    public static void exportExcelWithCustomHeaders(String sheetName, String filePath,
                                                    List<Map<String, Object>> data,
                                                    List<Map<String, String>> headers) throws IOException {

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet(sheetName);

        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.size(); i++) {
            Map<String, String> headerMap = headers.get(i);
            String key = headerMap.keySet().iterator().next();
            String name = headerMap.get(key);

            Cell cell = headerRow.createCell(i);
            cell.setCellValue(name);
            cell.setCellStyle(headerStyle);

            // 写入数据
            for (int rowIndex = 0; rowIndex < data.size(); rowIndex++) {
                Row row = sheet.getRow(rowIndex + 1);
                if (row == null) row = sheet.createRow(rowIndex + 1);

                Map<String, Object> record = data.get(rowIndex);
                Object value = record.get(key);

                Cell dataCell = row.createCell(i);
                if (value instanceof Number) {
                    dataCell.setCellValue(((Number) value).doubleValue());
                } else if (value != null) {
                    dataCell.setCellValue(value.toString());
                } else {
                    dataCell.setCellValue("");
                }
            }
        }

        for (int i = 0; i < headers.size(); i++) {
            sheet.autoSizeColumn(i);
        }

        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            workbook.write(fos);
        }
        workbook.close();
    }



    /**
     * Convert List<Map<String, Object>> to Excel file and save to specified path
     *
     * @param dataList List of data maps to be exported
     * @param filePath Output file path (should end with .xls)
     * @throws IOException If file writing fails
     */
    public static void exportToExcelFile(List<Map<String, Object>> dataList, String filePath) throws IOException {
        exportToExcelFile(dataList, filePath, "Sheet1");
    }

    /**
     * Convert List<Map<String, Object>> to Excel file and save to specified path with custom sheet name
     *
     * @param dataList List of data maps to be exported
     * @param filePath Output file path (should end with .xls)
     * @param sheetName Name of the Excel sheet
     * @throws IOException If file writing fails
     */
    public static void exportToExcelFile(List<Map<String, Object>> dataList, String filePath, String sheetName) throws IOException {
        // Check if data list is empty
        if (dataList == null || dataList.isEmpty()) {
            throw new IllegalArgumentException("Data list cannot be null or empty");
        }

        // Validate file path
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("File path cannot be null or empty");
        }

        HSSFWorkbook workbook = null;
        java.io.FileOutputStream fileOutputStream = null;

        try {
            // Create workbook and sheet
            workbook = new HSSFWorkbook();
            Sheet sheet = workbook.createSheet(sheetName);

            // Extract all unique keys from maps as column headers
            Set<String> headers = extractHeaders(dataList);

            // Create header row with styling
            createHeaderRow(sheet, headers, workbook);

            // Create data rows
            createDataRows(sheet, dataList, headers, workbook);

            // Auto-size columns for better readability
            autoSizeColumns(sheet, headers.size());

            // Create directories if they don't exist
            java.io.File file = new java.io.File(filePath);
            java.io.File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            // Write to file
            fileOutputStream = new java.io.FileOutputStream(filePath);
            workbook.write(fileOutputStream);
            fileOutputStream.flush();

        } finally {
            // Clean up resources manually
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    System.err.println("Warning: Failed to close FileOutputStream: " + e.getMessage());
                }
            }
            if (workbook != null) {
                try {
                    workbook.close();
                } catch (IOException e) {
                    System.err.println("Warning: Failed to close workbook: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Convert List<Map<String, Object>> to Excel base64 string
     *
     * @param dataList List of data maps to be exported
     * @return Base64 encoded Excel file content (.xls format)
     * @throws IOException If Excel generation fails
     */
    public static String exportToExcelBase64(List<Map<String, Object>> dataList) throws IOException {
        return exportToExcelBase64(dataList, "Sheet1");
    }

    /**
     * Convert List<Map<String, Object>> to Excel base64 string with custom sheet name
     *
     * @param dataList List of data maps to be exported
     * @param sheetName Name of the Excel sheet
     * @return Base64 encoded Excel file content (.xls format)
     * @throws IOException If Excel generation fails
     */
    public static String exportToExcelBase64(List<Map<String, Object>> dataList, String sheetName) throws IOException {
        // Check if data list is empty
        if (dataList == null || dataList.isEmpty()) {
            throw new IllegalArgumentException("Data list cannot be null or empty");
        }

        HSSFWorkbook workbook = null;
        ByteArrayOutputStream outputStream = null;

        try {
            // Create workbook and sheet using older compatible approach
            workbook = new HSSFWorkbook();
            Sheet sheet = workbook.createSheet(sheetName);

            // Extract all unique keys from maps as column headers
            Set<String> headers = extractHeaders(dataList);

            // Create header row with styling
            createHeaderRow(sheet, headers, workbook);

            // Create data rows
            createDataRows(sheet, dataList, headers, workbook);

            // Auto-size columns for better readability
            autoSizeColumns(sheet, headers.size());

            // Convert to base64 using manual approach to avoid Commons IO issues
            outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            outputStream.flush();

            byte[] excelBytes = outputStream.toByteArray();
            return Base64.getEncoder().encodeToString(excelBytes);

        } finally {
            // Clean up resources manually
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    System.err.println("Warning: Failed to close ByteArrayOutputStream: " + e.getMessage());
                }
            }
            if (workbook != null) {
                try {
                    workbook.close();
                } catch (IOException e) {
                    System.err.println("Warning: Failed to close workbook: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Extract all unique keys from the list of maps to use as column headers
     *
     * @param dataList List of data maps
     * @return Set of unique header names
     */
    private static Set<String> extractHeaders(List<Map<String, Object>> dataList) {
        Set<String> headers = new LinkedHashSet<>();
        for (Map<String, Object> dataMap : dataList) {
            if (dataMap != null) {
                headers.addAll(dataMap.keySet());
            }
        }
        return headers;
    }

    /**
     * Create header row with styling
     *
     * @param sheet Excel sheet
     * @param headers Set of header names
     * @param workbook Workbook instance for styling
     */
    private static void createHeaderRow(Sheet sheet, Set<String> headers, Workbook workbook) {
        Row headerRow = sheet.createRow(0);

        // Create header cell style
        CellStyle headerStyle = createHeaderCellStyle(workbook);

        int columnIndex = 0;
        for (String header : headers) {
            Cell cell = headerRow.createCell(columnIndex++);
            cell.setCellValue(header);
            cell.setCellStyle(headerStyle);
        }
    }

    /**
     * Create data rows
     *
     * @param sheet Excel sheet
     * @param dataList List of data maps
     * @param headers Set of header names
     * @param workbook Workbook instance for styling
     */
    private static void createDataRows(Sheet sheet, List<Map<String, Object>> dataList,
                                       Set<String> headers, Workbook workbook) {
        // Create date cell style for date formatting
        CellStyle dateCellStyle = createDateCellStyle(workbook);

        int rowIndex = 1; // Start from row 1 (row 0 is header)

        for (Map<String, Object> dataMap : dataList) {
            if (dataMap == null) continue;

            Row dataRow = sheet.createRow(rowIndex++);
            int columnIndex = 0;

            for (String header : headers) {
                Cell cell = dataRow.createCell(columnIndex++);
                Object value = dataMap.get(header);

                // Set cell value based on data type
                setCellValue(cell, value, dateCellStyle);
            }
        }
    }

    /**
     * Set cell value based on the object type
     *
     * @param cell Excel cell
     * @param value Object value to be set
     * @param dateCellStyle Cell style for date formatting
     */
    private static void setCellValue(Cell cell, Object value, CellStyle dateCellStyle) {
        if (value == null) {
            cell.setCellValue("");
        } else if (value instanceof String) {
            cell.setCellValue((String) value);
        } else if (value instanceof Integer) {
            cell.setCellValue(((Integer) value).doubleValue());
        } else if (value instanceof Long) {
            cell.setCellValue(((Long) value).doubleValue());
        } else if (value instanceof Double) {
            cell.setCellValue((Double) value);
        } else if (value instanceof Float) {
            cell.setCellValue(((Float) value).doubleValue());
        } else if (value instanceof Boolean) {
            cell.setCellValue((Boolean) value);
        } else if (value instanceof Date) {
            cell.setCellValue((Date) value);
            cell.setCellStyle(dateCellStyle);
        } else {
            // Convert other types to string
            cell.setCellValue(value.toString());
        }
    }

    /**
     * Create header cell style with bold font and background color
     * Compatible with older POI versions
     *
     * @param workbook Workbook instance
     * @return CellStyle for headers
     */
    private static CellStyle createHeaderCellStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();

        // Set background color (using compatible approach)
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // Set border (compatible with older versions)
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        // Create bold font
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);

        // Center alignment
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        return style;
    }

    /**
     * Create date cell style with custom date format
     *
     * @param workbook Workbook instance
     * @return CellStyle for date cells
     */
    private static CellStyle createDateCellStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        CreationHelper createHelper = workbook.getCreationHelper();
        style.setDataFormat(createHelper.createDataFormat().getFormat(DEFAULT_DATE_FORMAT));
        return style;
    }

    /**
     * Auto-size columns for better readability
     *
     * @param sheet Excel sheet
     * @param columnCount Number of columns
     */
    private static void autoSizeColumns(Sheet sheet, int columnCount) {
        for (int i = 0; i < columnCount; i++) {
            try {
                sheet.autoSizeColumn(i);
                // Set minimum column width to avoid too narrow columns
                int currentWidth = sheet.getColumnWidth(i);
                if (currentWidth < 2000) { // Minimum width
                    sheet.setColumnWidth(i, 2000);
                }
            } catch (Exception e) {
                // If autoSizeColumn fails, set a default width
                sheet.setColumnWidth(i, 3000);
            }
        }
    }
}