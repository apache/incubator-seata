package com.alibaba.fescar.order.service.impl;

import com.alibaba.fescar.order.feign.AccountApiFeign;
import com.alibaba.fescar.order.feign.AccountVO;
import com.alibaba.fescar.order.mapper.OrderDOMapper;
import com.alibaba.fescar.order.model.dao.OrderDO;
import com.alibaba.fescar.order.model.vo.OrderVO;
import com.alibaba.fescar.order.service.IOrderService;
import com.alibaba.fescar.spring.annotation.GlobalTransactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * @author Loki
 */
@Service
public class OrderServiceImpl implements IOrderService {

    @Autowired
    OrderDOMapper orderDOMapper;


    @Autowired
    AccountApiFeign accountApiFeign;


    @GlobalTransactional
    @Override
    public String create(OrderVO order) {
        OrderDO orderDO = new OrderDO();
        orderDO.setOrderCode(UUID.randomUUID().toString());
        orderDO.setOrderStatus((byte) 2);
        orderDOMapper.insert(orderDO);

        AccountVO account = new AccountVO();
        account.setMoney(order.getMoney());
        accountApiFeign.create(account);
        return null;
    }
}
