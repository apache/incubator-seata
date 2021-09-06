/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.demo.controller;


import com.demo.common.R;
import com.demo.model.StorageTbl;
import com.demo.service.ProviderService;
import io.seata.spring.annotation.GlobalTransactional;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@Api
@RequestMapping("/consumer")
public class ConsumerController {

    @Autowired
    private ProviderService providerService;

    private final static Logger logger = LoggerFactory.getLogger(ConsumerController.class);


    @GlobalTransactional
    @ApiOperation(value = "delCommdity")
    @GetMapping(value = "commodity/{commodityCode}")
    public R delCount(@PathVariable String commodityCode){
        providerService.subCount(commodityCode);
        return R.ok();
    }

    // 有错误的代码
    @GlobalTransactional
    @GetMapping(value = "commodityWrong/{commodityCode}")
    @ApiOperation(value = "delCountWrong")
    public R delCountWrong(@PathVariable String commodityCode){
        providerService.subCount(commodityCode);
        int i = 1 / 0;
        return R.ok();
    }

    @PutMapping("commodity")
    @GlobalTransactional
    @ApiOperation(value = "addCommodity")
    public R addCommodity(@RequestBody StorageTbl storageTbl){
        providerService.addCommodity(storageTbl);
        return R.ok();
    }

    @ApiOperation(value = "queryCount")
    @GetMapping("{commodityCode}/count")
    public R queryCount(@PathVariable String commodityCode) {
        R r = providerService.queryCount(commodityCode);
        Integer count = (Integer) r.getData().get("count");
        return R.ok().data("count",count);
    }

}