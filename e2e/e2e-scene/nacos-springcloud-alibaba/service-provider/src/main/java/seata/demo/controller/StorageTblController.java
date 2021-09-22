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

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import seata.demo.common.R;
import seata.demo.model.StorageTbl;
import seata.demo.service.StorageTblService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author jingliu_xiong@foxmail.com
 */
@RestController
@RequestMapping("/storage")
public class StorageTblController {

    @Autowired
    private StorageTblService storageTblService;

    @GetMapping(value = "commodity/{commodityCode}")
    public R subCount(@PathVariable String commodityCode) {
        storageTblService.delCount(commodityCode);
        return R.ok();

    }

    @PutMapping("commodity")
    public R addCommodity(@RequestBody StorageTbl storageTbl) {
        storageTblService.save(storageTbl);
        return R.ok();
    }

    @GetMapping("{commodityCode}/count")
    public R queryCount(@PathVariable String commodityCode) {
        Integer count = storageTblService.queryCount(commodityCode);
        return R.ok().data("count",count);
    }


}