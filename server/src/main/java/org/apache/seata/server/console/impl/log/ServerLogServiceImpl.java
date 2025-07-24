package org.apache.seata.server.console.impl.log;

import org.apache.commons.io.input.ReversedLinesFileReader;
import org.apache.seata.common.result.SingleResult;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.core.constants.ConfigurationKeys;
import org.apache.seata.server.console.entity.param.ServerLogParam;
import org.apache.seata.server.console.entity.vo.ServerLogVO;
import org.apache.seata.server.console.service.ServerLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class ServerLogServiceImpl implements ServerLogService {

    @Autowired
    private Environment env;

    private static final String serverAllLog = "all";

    private static final String serverWarnLog = "warn";

    private static final String serverErrorLog = "error";

    private static final long timeoutMillis = 10L;

    private static final Integer maxLines = 50;

    private final Logger LOGGER = LoggerFactory.getLogger(ServerLogServiceImpl.class);

    @Override
    public SingleResult<?> getServerLog(ServerLogParam serverLogParam) {
        String logTime = serverLogParam.getLogTime();
        String logType = serverLogParam.getLogType();
        Integer cursor = serverLogParam.getCursor();
        Long costedTime = serverLogParam.getCostedTime();
        Integer nextLines = serverLogParam.getNextLines();
        if(nextLines>maxLines){
            return SingleResult.failure("Exceeds the maximum number of reads in a single session");
        }
        if(!logType.equals(serverAllLog) && !logType.equals(serverWarnLog) && !logType.equals(serverErrorLog)){
            return null;
        }
        String logFilePath = env.getProperty("logging.file.path") + "/" + env.getProperty("spring.application.name","seata-server") + "."
                + System.getProperty(ConfigurationKeys.SERVER_SERVICE_PORT_CAMEL) + "." + logType + ".log";
        File logFile = new File(logFilePath);
        if(cursor==null || cursor==0){
            costedTime = 0L;
            if(cursor==null){
                cursor = 0;
            }
        }
        List<String> logMessages = new ArrayList<>();
        try(ReversedLinesFileReader reader = new ReversedLinesFileReader(logFile, StandardCharsets.UTF_8)){
            int lineCount = 0;
            while(costedTime<=timeoutMillis && lineCount<=nextLines){
                long startTime = System.currentTimeMillis();
                String logMessage = reader.toString(lineCount++);
                if(StringUtils.isNotBlank(logMessage)) logMessages.add(logMessage);
                costedTime += System.currentTimeMillis() - startTime;
            }
            cursor += lineCount;
        } catch (Exception e){
            LOGGER.error("Read ${} failed: ${}", logFilePath,e.getMessage());
        }
        ServerLogVO logVO = new ServerLogVO(cursor,costedTime,logMessages);
        return SingleResult.success(logVO);
    }
}
