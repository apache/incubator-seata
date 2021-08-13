package com.demo.service;

import com.demo.model.StorageTbl;
import com.baomidou.mybatisplus.extension.service.IService;
import io.swagger.models.auth.In;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author ${author}
 * @since 2021-07-03
 */
public interface StorageTblService extends IService<StorageTbl> {

    void delCount(String commodityCode);

    Integer queryCount(String commodityCode);
}
