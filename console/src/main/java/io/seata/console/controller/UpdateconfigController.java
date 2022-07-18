package io.seata.console.controller;

import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.console.constant.Code;
import io.seata.console.result.SingleResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author:Yuzhiqiang
 * @Description:
 * @Date: Create in 13:42 2022/6/24
 * @Modified By:
 */
@RestController
@RequestMapping(value = "/api/v1/editconfig")
public class UpdateconfigController {

    private static final Configuration CONFIG = ConfigurationFactory.getInstance();

//    private static final GlobalTransaction TRANSACTION = GlobalTransactionContext.getCurrentOrCreate();

    @RequestMapping(value = "/putconfig", method = RequestMethod.POST)
    public SingleResult<Boolean> putconfig(String dataId, String content)  {

        try {
            Boolean result = CONFIG.putConfig(dataId, content);
            if(result) {
                return SingleResult.success(result);
            } else {
                return SingleResult.failure(Code.ERROR);
            }
        } catch(Exception e) {
            e.printStackTrace();
            return SingleResult.failure("101", "修改配置异常");
        }
    }

    @RequestMapping(value = "/getconfig", method = RequestMethod.POST)
    public String get(String dataId)  {
        String configFromSys = CONFIG.getConfig(dataId);
        return configFromSys;
    }

}
