package org.apache.seata.server.console.param;

import java.util.List;
import java.util.Map;

public class ParamUtil {
    public static String getStringParam(Map<String, List<String>> paramMap, String param) {
        List<String> valueList = paramMap.get(param);
        if (valueList != null && !valueList.isEmpty()) {
            return valueList.get(0);
        }

        return "";
    }

    public static Long getLongParam(Map<String, List<String>> paramMap, String param) {
        List<String> valueList = paramMap.get(param);
        if (valueList != null && !valueList.isEmpty()) {
            return Long.parseLong(valueList.get(0));
        }

        return 0L;
    }

    public static Integer getIntParam(Map<String, List<String>> paramMap, String param) {
        List<String> valueList = paramMap.get(param);
        if (valueList != null && !valueList.isEmpty()) {
            return Integer.parseInt(valueList.get(0));
        }

        return 0;
    }

    public static Boolean getBooleanParam(Map<String, List<String>> paramMap, String param) {
        List<String> valueList = paramMap.get(param);
        if (valueList != null && !valueList.isEmpty()) {
            return Boolean.parseBoolean(valueList.get(0));
        }
        
        return false;
    }
}