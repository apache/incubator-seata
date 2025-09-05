package org.apache.seata.mcp.service.impl;

import org.apache.seata.common.result.PageResult;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.mcp.entity.constant.RPCConstant;
import org.apache.seata.mcp.entity.param.ServerLogParam;
import org.apache.seata.mcp.entity.pojo.NameSpaceDetail;
import org.apache.seata.mcp.entity.pojo.ServerLogDetails;
import org.apache.seata.mcp.service.MCPRPCService;
import org.apache.seata.mcp.service.ServerLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class ServerLogServiceImpl implements ServerLogService {

    @Autowired
    private MCPRPCService mcpRPCService;

    private static final int SERVER_LOG_PAGE_SIZE = 2500;

    @Override
    public PageResult<String> analyseServerLogFile(NameSpaceDetail nameSpaceDetail, ServerLogParam param) {

        checkLogParam(param);

        ServerLogDetails logsDetails = mcpRPCService.getCallTCLogs(
                nameSpaceDetail, RPCConstant.SERVER_LOG_BASE_URL + "/getCurrentServerLogFile", param, null, null);

        int pageNum = param.getPage();

        Pattern timestampPattern = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}:\\d{2}\\.\\d{3}");

        // One-time completion: Build entries → filter → count → pagination
        PaginationResult result = logsDetails
                .getLogs()
                .collect(
                        () -> new FilteringLogAccumulator(param, timestampPattern),
                        (accumulator, line) -> accumulator.processLine(line))
                .map(accumulator -> {
                    accumulator.finish();

                    PaginationResult paginationResult = new PaginationResult();
                    int skipCounts = (pageNum - 1) * SERVER_LOG_PAGE_SIZE;

                    int index = 0;
                    for (String entry : accumulator.drainFilteredEntries()) {
                        paginationResult.incrementTotal();
                        if (index >= skipCounts
                                && paginationResult.getPageEntries().size() < SERVER_LOG_PAGE_SIZE) {
                            paginationResult.addToPage(entry);
                        }
                        index++;
                    }
                    return paginationResult;
                })
                .blockOptional()
                .orElse(new PaginationResult());

        return PageResult.success(result.getPageEntries(), result.getTotalCount(), pageNum, SERVER_LOG_PAGE_SIZE);
    }

    private void checkLogParam(ServerLogParam logParam) {
        Integer page = logParam.getPage();
        List<String> logMessageKeyWord = logParam.getLogMessageKeyWord();
        String logType = logParam.getLogType();
        List<String> logMessageTime = logParam.getLogMessageTime();
        String logMessageLevel = logParam.getLogMessageLevel();

        if ((logMessageKeyWord == null || logMessageKeyWord.isEmpty())
                && (logMessageTime == null || logMessageTime.isEmpty())) {
            throw new IllegalArgumentException(
                    "It is not allowed to query logs without any parameters(except page and logType)");
        }

        if (page == null || page < 1) {
            throw new IllegalArgumentException("The page number must be greater than or equal to 1");
        }

        if (StringUtils.isNotBlank(logType)) {
            if (!Arrays.asList("all", "error", "warn").contains(logType.toLowerCase())) {
                throw new IllegalArgumentException(
                        "The logType parameter value is invalid and must be: all, error, warn");
            }
        }

        if (StringUtils.isNotBlank(logMessageLevel)) {
            if (!Arrays.asList("error", "warn", "info").contains(logMessageLevel.toLowerCase())) {
                throw new IllegalArgumentException(
                        "The logMessageLevel parameter value is invalid and must be: error, warn, info");
            }
        }
    }

    private static class FilteringLogAccumulator {
        private final ServerLogParam param;
        private final Pattern timestampPattern;
        private StringBuilder currentEntry = new StringBuilder();
        private final List<String> readyEntries = new ArrayList<>();

        public FilteringLogAccumulator(ServerLogParam param, Pattern timestampPattern) {
            this.param = param;
            this.timestampPattern = timestampPattern;
        }

        public void processLine(String line) {
            if (timestampPattern.matcher(line).find()) {
                flushCurrentEntry();
                currentEntry = new StringBuilder(line);
            } else {
                if (currentEntry.length() > 0) {
                    currentEntry.append("\n").append(line);
                }
            }
        }

        public void finish() {
            flushCurrentEntry();
        }

        private void flushCurrentEntry() {
            if (currentEntry.length() > 0) {
                String completeEntry = currentEntry.toString().trim();
                if (matchesFilter(completeEntry)) {
                    readyEntries.add(completeEntry);
                }
            }
        }

        private boolean matchesFilter(String entry) {
            // Filter by level
            if (param.getLogMessageLevel() != null
                    && !entry.toUpperCase().contains(param.getLogMessageLevel().toUpperCase())) {
                return false;
            }
            // Filter by keyword
            if (param.getLogMessageKeyWord() != null
                    && !param.getLogMessageKeyWord().isEmpty()) {
                boolean matched = false;
                for (String key : param.getLogMessageKeyWord()) {
                    if (entry.contains(key)) {
                        matched = true;
                        break;
                    }
                }
                if (!matched) return false;
            }
            // Filter by time
            if (param.getLogMessageTime() != null && !param.getLogMessageTime().isEmpty()) {
                boolean matched = false;
                for (String time : param.getLogMessageTime()) {
                    if (entry.contains(time)) {
                        matched = true;
                        break;
                    }
                }
                return matched;
            }
            return true;
        }

        public List<String> drainFilteredEntries() {
            List<String> copy = new ArrayList<>(readyEntries);
            readyEntries.clear();
            return copy;
        }
    }

    private static class PaginationResult {
        private final List<String> pageEntries = new ArrayList<>();
        private int totalCount = 0;
        private int currentIndex = 0;

        public void incrementTotal() {
            totalCount++;
        }

        public boolean shouldIncludeInPage(int skipCounts, int pageSize) {
            boolean shouldInclude = currentIndex >= skipCounts && pageEntries.size() < pageSize;
            currentIndex++;
            return shouldInclude;
        }

        public void addToPage(String entry) {
            pageEntries.add(entry);
        }

        public List<String> getPageEntries() {
            return pageEntries;
        }

        public int getTotalCount() {
            return totalCount;
        }
    }
}
