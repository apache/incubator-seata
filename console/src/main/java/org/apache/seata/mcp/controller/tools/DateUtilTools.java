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
package org.apache.seata.mcp.controller.tools;

import org.apache.seata.mcp.annotation.Tool;
import org.apache.seata.mcp.annotation.ToolParam;
import org.apache.seata.mcp.utils.DateUtils;
import org.springframework.stereotype.Service;

@Service
public class DateUtilTools {

    @Tool(description = "Convert from dateTime format to the corresponding timestamp")
    public Long covertToTimestampFromDateTimeString(
            @ToolParam(description = "dateTime, The format is yyyy-MM-dd HH:mm:ss") String dateTime) {
        return DateUtils.convertToTimeStampFromDateTime(dateTime);
    }

    @Tool(description = "Convert from date format to the corresponding timestamp")
    public Long covertToTimestampFromDateString(
            @ToolParam(description = "date, The format is yyyy-MM-dd") String date) {
        return DateUtils.convertToTimestampFromDate(date);
    }

    @Tool(description = "Convert from timestamp to the corresponding date format")
    public String covertToDateTimeFromTimestamp(@ToolParam(description = "TimeStamp") Long timestamp) {
        return DateUtils.convertToDateTimeFromTimestamp(timestamp);
    }
}
