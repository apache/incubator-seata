package org.apache.seata.mcp.controller.tools;

import org.apache.seata.mcp.annotation.Tool;
import org.apache.seata.mcp.annotation.ToolParam;
import org.apache.seata.mcp.utils.DateUtils;
import org.springframework.stereotype.Service;

@Service
public class DateUtilTools {

    @Tool(description = "Convert from dateTime format to the corresponding timestamp")
    public Long covertToTimestampFromDateTimeString(@ToolParam(description = "dateTime, The format is yyyy-MM-dd HH:mm:ss") String dateTime){
        return DateUtils.convertToTimeStampFromDateTime(dateTime);
    }

    @Tool(description = "Convert from date format to the corresponding timestamp")
    public Long covertToTimestampFromDateString(@ToolParam(description = "date, The format is yyyy-MM-dd") String date){
        return DateUtils.convertToTimestampFromDate(date);
    }
}
