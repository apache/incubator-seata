package com.vergilyn.examples.order.service;

import com.vergilyn.examples.dto.OrderDTO;
import com.vergilyn.examples.response.ObjectResponse;

public interface OrderService {

    /** 创建订单 */
    ObjectResponse<OrderDTO> create(String userId, String commodityCode, Integer orderTotal, Double orderAmount);

    ObjectResponse<Long> count();

}
