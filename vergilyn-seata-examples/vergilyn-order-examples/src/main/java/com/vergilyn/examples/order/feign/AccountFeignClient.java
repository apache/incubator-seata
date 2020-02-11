package com.vergilyn.examples.order.feign;

import com.vergilyn.examples.constants.NacosConstant;
import com.vergilyn.examples.response.ObjectResponse;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = NacosConstant.APPLICATION_ACCOUNT)
public interface AccountFeignClient {

    @RequestMapping(path = "/account/decrease", method = RequestMethod.GET)
    ObjectResponse<Void> decrease(@RequestParam("userId") String userId, @RequestParam("amount") Double amount);
}