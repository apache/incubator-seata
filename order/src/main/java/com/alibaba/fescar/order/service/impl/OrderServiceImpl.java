package com.alibaba.fescar.order.service.impl;

import com.alibaba.fescar.order.model.vo.OrderVO;
import com.alibaba.fescar.order.service.IOrderService;
import com.alibaba.fescar.spring.annotation.GlobalTransactional;
import org.springframework.stereotype.Service;

/**
 * @author Loki
 */
@Service
public class OrderServiceImpl implements IOrderService {


    @GlobalTransactional
    @Override
    public String create(OrderVO order) {
        return null;
    }
}
