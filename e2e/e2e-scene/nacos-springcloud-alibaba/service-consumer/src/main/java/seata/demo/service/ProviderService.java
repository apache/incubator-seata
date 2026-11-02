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

package seata.demo.service;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import seata.demo.common.R;
import seata.demo.model.StorageTbl;
import seata.demo.service.failback.ProviderFailBack;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * @author jingliu_xiong@foxmail.com
 */
@FeignClient(value = "provider-service", fallback = ProviderFailBack.class)
public interface ProviderService {

    @GetMapping(value = "storage/commodity/{commodityCode}")
    public R subCount(@PathVariable String commodityCode);

    @PutMapping("storage/commodity")
    public R addCommodity(@RequestBody StorageTbl storageTbl);

    @GetMapping("storage/{commodityCode}/count")
    public R queryCount(@PathVariable String commodityCode);
}