package com.alibaba.fescar.order;

import com.alibaba.fescar.order.model.vo.OrderVO;
import com.alibaba.fescar.order.service.IOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "order")
public class OrderController {

    @Autowired
    private IOrderService orderService;

    @RequestMapping(value = "create", method = RequestMethod.POST)
    public void create(@RequestBody OrderVO order) {
        String orderId = orderService.create(order);
    }
}
