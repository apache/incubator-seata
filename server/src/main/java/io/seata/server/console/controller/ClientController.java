package io.seata.server.console.controller;

import io.seata.console.result.PageResult;
import io.seata.server.console.param.ClientQueryParam;
import io.seata.server.console.service.ClientService;
import io.seata.server.console.vo.ClientVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/api/v1/console/client")
public class ClientController {

    @Resource
    ClientService clientService;

    @GetMapping("/query")
    public PageResult<ClientVO> query(@ModelAttribute ClientQueryParam param) {
        return clientService.query(param);
    }

}
