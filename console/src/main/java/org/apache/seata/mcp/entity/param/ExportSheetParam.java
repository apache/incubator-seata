package org.apache.seata.mcp.entity.param;

import org.apache.seata.mcp.annotation.ToolParam;

import java.util.List;
import java.util.Map;

public class ExportSheetParam {

    @ToolParam(description = "The input data is of type List<Map<String, Object>>",required = true)
    private List<Map<String,Object>> data;
    @ToolParam(description = "Sheet Name",required = true)
    private String sheetName;
    @ToolParam(description = "Where the file is stored, Be sure to ask the user where they want to store it, The default is the system user folder")
    private String filePath;

    @Override
    public String toString() {
        return "ExportSheetParam{" +
                "data=" + data +
                ", sheetName='" + sheetName + '\'' +
                ", filePath='" + filePath + '\'' +
                '}';
    }

    public List<Map<String, Object>> getData() {
        return data;
    }

    public void setData(List<Map<String, Object>> data) {
        this.data = data;
    }

    public String getSheetName() {
        return sheetName;
    }

    public void setSheetName(String sheetName) {
        this.sheetName = sheetName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
}
