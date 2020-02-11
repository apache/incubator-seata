package com.vergilyn.examples.order.service.impl;

import javax.transaction.Transactional;

import com.vergilyn.examples.dto.OrderDTO;
import com.vergilyn.examples.enums.RspStatusEnum;
import com.vergilyn.examples.order.entity.Order;
import com.vergilyn.examples.order.feign.AccountFeignClient;
import com.vergilyn.examples.order.feign.StorageFeignClient;
import com.vergilyn.examples.order.repository.OrderRepository;
import com.vergilyn.examples.order.service.OrderService;
import com.vergilyn.examples.response.ObjectResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OrderServiceImpl implements OrderService {
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private AccountFeignClient accountFeignClient;
    @Autowired
    private StorageFeignClient storageFeignClient;

    @Override
    @Transactional
    public ObjectResponse<OrderDTO> create(String userId, String commodityCode, Integer orderTotal, Double orderAmount) {

        ObjectResponse<OrderDTO> response = new ObjectResponse<>();
        // 扣减用户账户
        ObjectResponse<Void> accountResp = accountFeignClient.decrease(userId, orderAmount);

        // 扣减库存
        ObjectResponse<Void> storageResp = storageFeignClient.decrease(commodityCode, orderTotal);

        try {
            //生成订单
            Order order = new Order(userId, commodityCode, orderTotal, orderAmount);
            orderRepository.save(order);
        } catch (Exception e) {
            return response.result(RspStatusEnum.FAIL);
        }

        if (accountResp.getStatus() != 200
              || storageResp.getStatus() != 200) {
            return response.result(RspStatusEnum.FAIL);
        }

        return response.result(RspStatusEnum.SUCCESS);
    }

    @Override
    public ObjectResponse<Long> count() {
        return ObjectResponse.success(orderRepository.count());
    }
}
