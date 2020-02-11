package com.vergilyn.examples.storage.service.impl;

import javax.transaction.Transactional;

import com.vergilyn.examples.response.ObjectResponse;
import com.vergilyn.examples.storage.entity.Storage;
import com.vergilyn.examples.storage.repository.StorageRepository;
import com.vergilyn.examples.storage.service.StorageService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StorageServiceImpl implements StorageService {
    @Autowired
    private StorageRepository storageRepository;

    @Override
    @Transactional
    public ObjectResponse<Void> decrease(String commodityCode, int total) {

        int storage = storageRepository.decreaseStorage(commodityCode, total);

        return storage > 0 ? ObjectResponse.success() : ObjectResponse.failure();
    }

    @Override
    public ObjectResponse<Storage> getByCommodityCode(String code) {
        Storage storage = storageRepository.getFirstByCommodityCode(code);
        return ObjectResponse.success(storage);
    }
}
