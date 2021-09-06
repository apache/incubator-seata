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
import com.demo.service.StorageTblService;
// import io.seata.spring.annotation.GlobalTransactional;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author ${author}
 * @since 2021-07-03
 */
@RestController
@RequestMapping("/storage")
@Api
public class StorageTblController {

    @Autowired
    private StorageTblService storageTblService;

    @GetMapping(value = "commodity/{commodityCode}")
    @ApiOperation(value = "subCommdityCount")
    public R subCount(@PathVariable String commodityCode) {
        storageTblService.delCount(commodityCode);
        return R.ok();

    }

    @ApiOperation(value = "addCommdity")
    @PutMapping("commodity")
    public R addCommodity(@RequestBody StorageTbl storageTbl) {
        storageTblService.save(storageTbl);
        return R.ok();
    }

    @ApiOperation(value = "queryCount")
    @GetMapping("{commodityCode}/count")
    public R queryCount(@PathVariable String commodityCode) {
        Integer count = storageTblService.queryCount(commodityCode);
        return R.ok().data("count",count);
    }


}