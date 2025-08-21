package org.apache.seata.mcp.controller.tools;

import org.apache.seata.common.result.SingleResult;
import org.apache.seata.mcp.annotation.Tool;
import org.apache.seata.mcp.annotation.ToolParam;
import org.apache.seata.mcp.entity.param.ExportSheetParam;
import org.apache.seata.mcp.utils.ExcelExportUtil;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
public class ExportTools {

    @Tool(description = "Automatically convert the queried data into an Excel table and store it in the specified location with custom headers, Please show it directly to the user when it is successful")
    public SingleResult<?> exportExcelFromDataWithCustomHeaders(
            @ToolParam(description = "The export sheet param",required = true) ExportSheetParam param,
            @ToolParam(description = "Customize the excel header is of type List<Map<String, String>>," +
                    "key is the name of the field used in the data, value is the name that appears on the excel header",required = true) List<Map<String, String>> headers
                                                                ) throws IOException {
        ExcelExportUtil.exportExcelWithCustomHeaders(param,headers);
        return SingleResult.success("Conversion is successful, please check the specified file location: "+param.getFilePath());
    }

}
