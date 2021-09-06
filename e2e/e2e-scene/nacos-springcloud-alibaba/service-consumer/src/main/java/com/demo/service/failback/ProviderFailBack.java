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

package com.demo.service.failback;

import com.demo.common.R;
import com.demo.model.StorageTbl;
import com.demo.service.ProviderService;
import org.springframework.stereotype.Component;

@Component
public class ProviderFailBack implements ProviderService {

    @Override
    public R subCount(String commodityCode) {
        return R.error();
    }

    @Override
    public R addCommodity(StorageTbl storageTbl) {
        return R.error();
    }

    @Override
    public R queryCount(String commodityCode) {
        return R.error();
    }
}