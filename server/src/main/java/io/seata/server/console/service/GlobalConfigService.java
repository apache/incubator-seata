package io.seata.server.console.service;

import io.seata.console.result.PageResult;
import io.seata.server.console.vo.GlobalConfigVO;
import org.springframework.stereotype.Service;

/**
 * @Author:Yuzhiqiang
 * @Description:
 * @Date: Create in 14:48 2022/7/29
 * @Modified By:
 */
public interface GlobalConfigService {

    PageResult<GlobalConfigVO> getConfigList();
}
