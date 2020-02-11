package com.vergilyn.examples.order.feign;

import com.vergilyn.examples.constants.NacosConstant;
import com.vergilyn.examples.response.ObjectResponse;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author vergilyn
 * @date 2020-02-11
 */
@FeignClient(name = NacosConstant.APPLICATION_STORAGE)
public interface StorageFeignClient {

    @RequestMapping(path = "/storage/decrease", method = RequestMethod.POST)
    ObjectResponse<Void> decrease(@RequestParam("commodityCode") String commodityCode, @RequestParam("total") int total);
}