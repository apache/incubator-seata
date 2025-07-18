package org.apache.seata.server.console.impl.log;

import org.apache.seata.core.constants.ConfigurationKeys;
import org.apache.seata.server.console.entity.param.ServerLogParam;
import org.apache.seata.server.console.service.ServerLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class ServerLogServiceImpl implements ServerLogService {

    @Autowired
    private Environment env;

    private static final String serverAllLog = "all";

    private static final String serverWarnLog = "warn";

    private static final String serverErrorLog = "error";

    @Override
    public String getServerLog(ServerLogParam serverLogParam) {
        String logTime = serverLogParam.getLogTime();
        String logType = serverLogParam.getLogType();
        String logFileName = env.getProperty("logging.file.path") + "/" + env.getProperty("spring.application.name","seata-server") + "."
                + System.getProperty(ConfigurationKeys.SERVER_SERVICE_PORT_CAMEL) + "." + logType + ".log";
        return logFileName;
    }
}
