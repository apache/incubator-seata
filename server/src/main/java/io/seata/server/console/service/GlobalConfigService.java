package io.seata.server.console.service;

import io.seata.console.result.PageResult;
import io.seata.server.console.vo.GlobalConfigVO;

/**
 * Global config service
 * @author Yuzhiqiang
 */
public interface GlobalConfigService {

    /**
     * Query locks by param
     * @return the list of GlobalLockVO
     */
    PageResult<GlobalConfigVO> getConfigList();
}
