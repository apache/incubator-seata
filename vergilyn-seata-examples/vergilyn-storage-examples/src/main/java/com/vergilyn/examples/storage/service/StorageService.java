package com.vergilyn.examples.storage.service;

import com.vergilyn.examples.response.ObjectResponse;
import com.vergilyn.examples.storage.entity.Storage;

public interface StorageService {

    /**
     * 扣减库存
     * @param commodityCode
     * @param total
     */
    ObjectResponse<Void> decrease(String commodityCode, int total);

    /**
     * 获取库存
     */
    ObjectResponse<Storage> getByCommodityCode(String code);
}
