package com.alibaba.fescar.order.service;

import com.alibaba.fescar.order.model.vo.OrderVO;

/**
 * @author Loki
 */
public interface IOrderService {
    /**
     * create order
     *
     * @param order
     * @return
     */
    String create(OrderVO order);
}
