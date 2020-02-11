package com.vergilyn.examples.storage.controller;

import com.vergilyn.examples.response.ObjectResponse;
import com.vergilyn.examples.storage.entity.Storage;
import com.vergilyn.examples.storage.service.StorageService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/storage")
@RestController
@Slf4j
public class StorageController {
    @Autowired
    private StorageService storageService;

    /**
     * 扣减库存
     */
    @PostMapping("/decrease")
    ObjectResponse decreaseStorage(@RequestParam("commodityCode") String commodityCode, @RequestParam("total") int total){
        log.info("请求库存微服务 >>>> commodityCode = {}, total = {}", commodityCode, total);
        return storageService.decrease(commodityCode, total);
    }

    @GetMapping("/get")
    ObjectResponse<Storage> get(String commodityCode){
        return storageService.getByCommodityCode(commodityCode);
    }
}
