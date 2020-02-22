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

import io.seata.core.context.RootContext;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private AccountFeignClient accountFeignClient;
    @Autowired
    private StorageFeignClient storageFeignClient;

    @Override
    @Transactional
    @GlobalTransactional(name = "vergilyn-first-global-transaction", rollbackFor = Exception.class)
    public ObjectResponse<OrderDTO> create(String userId, String commodityCode, Integer orderTotal, Double orderAmount) {
        log.info("开启全局事务 >>>> xid: {}", RootContext.getXID());

        ObjectResponse<OrderDTO> response = new ObjectResponse<>();
       /* // 扣减用户账户
        ObjectResponse<Void> accountResp = accountFeignClient.decrease(userId, orderAmount);
        if (accountResp.getStatus() != 200){
            throw new DefaultException("请求account错误");
        }

        // 扣减库存
        ObjectResponse<Void> storageResp = storageFeignClient.decrease(commodityCode, orderTotal);
        if (storageResp.getStatus() != 200){
            throw new DefaultException("请求storage错误");
        }*/

        //生成订单
        Order order = new Order(userId, commodityCode, orderTotal, orderAmount);
        orderRepository.save(order);

        return response.result(RspStatusEnum.SUCCESS);
    }

    @Override
    public ObjectResponse<Long> count() {
        return ObjectResponse.success(orderRepository.count());
    }
}
