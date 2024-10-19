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

package seata.demo.controller;


import seata.demo.common.R;
import seata.demo.model.StorageTbl;
import seata.demo.service.ProviderService;
import io.seata.spring.annotation.GlobalTransactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @author jingliu_xiong@foxmail.com
 */
@RestController
@RequestMapping("/consumer")
public class ConsumerController {

    @Autowired
    private ProviderService providerService;

    private final static Logger logger = LoggerFactory.getLogger(ConsumerController.class);


    @GlobalTransactional
    @GetMapping(value = "commodity/{commodityCode}")
    public R delCount(@PathVariable String commodityCode){
        providerService.subCount(commodityCode);
        return R.ok();
    }

    /**
     * This code will throw an error when running, and then Seata will rollback
     * @param commodityCode
     * @return
     */
    @GlobalTransactional
    @GetMapping(value = "commodityWrong/{commodityCode}")
    public R delCountWrong(@PathVariable String commodityCode){
        providerService.subCount(commodityCode);
        int i = 1 / 0;
        return R.ok();
    }

    @PutMapping("commodity")
    @GlobalTransactional
    public R addCommodity(@RequestBody StorageTbl storageTbl){
        providerService.addCommodity(storageTbl);
        return R.ok();
    }

    @GetMapping("{commodityCode}/count")
    public R queryCount(@PathVariable String commodityCode) {
        R r = providerService.queryCount(commodityCode);
        Integer count = (Integer) r.getData().get("count");
        return R.ok().data("count",count);
    }

}