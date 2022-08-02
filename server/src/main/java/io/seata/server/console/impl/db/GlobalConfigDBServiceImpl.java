package io.seata.server.console.impl.db;

import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.console.result.PageResult;
import io.seata.server.console.service.GlobalConfigService;
import io.seata.server.console.vo.GlobalConfigVO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author:Yuzhiqiang
 * @Description:
 * @Date: Create in 14:50 2022/7/29
 * @Modified By:
 */
@Service
public class GlobalConfigDBServiceImpl implements GlobalConfigService {

    private static final Configuration CONFIG = ConfigurationFactory.getInstance();

    @Override
    public PageResult<GlobalConfigVO> getConfigList() {

        List<GlobalConfigVO> list = new ArrayList<>();
        String[] configKey = {"1", "2", "3", "4", "5", "6"};
        String[] descr = {"11111", "22222", "33333", "44444", "55555", "66666"};
        for(int i = 0; i < configKey.length; i++) {
            String config = configKey[i];
            String value = CONFIG.getConfig(config);
            GlobalConfigVO globalConfig = new GlobalConfigVO(String.valueOf(i), config, "value", descr[i]);
            list.add(globalConfig);
        }
        return PageResult.success(list, list.size(), 10, 1);
    }
}
