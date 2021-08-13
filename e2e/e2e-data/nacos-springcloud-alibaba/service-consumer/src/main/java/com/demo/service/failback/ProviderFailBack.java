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
