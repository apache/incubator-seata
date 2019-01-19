package com.alibaba.fescar.order.feign;

import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@FeignClient(value = "account-server", path = "account")
public interface AccountApiFeign {
    @RequestMapping(value = "create", method = RequestMethod.POST)
    public String create(@RequestBody AccountVO account);
}

@Component
class AccountApiFeignFallback implements AccountApiFeign {

    @Override
    public String create(AccountVO account) {
        return String.valueOf("0000000");
    }
}
