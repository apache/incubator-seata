package io.seata.server.console.service;


import io.seata.console.result.PageResult;
import io.seata.server.console.param.ClientQueryParam;
import io.seata.server.console.vo.ClientVO;

public interface ClientService {

    PageResult<ClientVO> query(ClientQueryParam param);

}
