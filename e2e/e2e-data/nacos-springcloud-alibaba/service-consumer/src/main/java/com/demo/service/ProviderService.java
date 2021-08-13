package com.demo.service;

import com.demo.common.R;
import com.demo.model.StorageTbl;
import com.demo.service.failback.ProviderFailBack;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(value = "provider-service", fallback = ProviderFailBack.class)
public interface ProviderService {

    @GetMapping(value = "storage/commodity/{commodityCode}")
    public R subCount(@PathVariable String commodityCode);

    @PutMapping("storage/commodity")
    public R addCommodity(@RequestBody StorageTbl storageTbl);

    @GetMapping("storage/{commodityCode}/count")
    public R queryCount(@PathVariable String commodityCode);
}
