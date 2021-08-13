package com.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.demo.model.StorageTbl;
import com.demo.mapper.StorageTblMapper;
import com.demo.service.StorageTblService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author ${author}
 * @since 2021-07-03
 */
@Service
public class StorageTblServiceImpl extends ServiceImpl<StorageTblMapper, StorageTbl> implements StorageTblService {

    @Override
    public void delCount(String commodityCode) {
        QueryWrapper<StorageTbl> storageTblWrapper = new QueryWrapper<>();
        storageTblWrapper.eq("commodity_code", commodityCode);
        StorageTbl one = baseMapper.selectOne(storageTblWrapper);
        one.setCount(one.getCount()-1);
        baseMapper.updateById(one);
    }

    @Override
    public Integer queryCount(String commodityCode) {
        QueryWrapper<StorageTbl> storageTblWrapper = new QueryWrapper<>();
        storageTblWrapper.eq("commodity_code", commodityCode);
        StorageTbl one = baseMapper.selectOne(storageTblWrapper);
        return one.getCount();
    }


}
