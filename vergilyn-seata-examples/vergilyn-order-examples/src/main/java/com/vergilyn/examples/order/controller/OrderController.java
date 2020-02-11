package com.vergilyn.examples.order.controller;

import com.vergilyn.examples.dto.OrderDTO;
import com.vergilyn.examples.order.service.OrderService;
import com.vergilyn.examples.response.ObjectResponse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/order")
@RestController
@Slf4j
public class OrderController {

    @Autowired
    private OrderService orderService;

    @RequestMapping("/create")
    public ObjectResponse<OrderDTO> createOrder(String userId, String commodityCode, Integer orderTotal, Double orderAmount){
        log.info("请求订单微服务 >>>> userId = {}, commodityCode = {}, orderTotal = {}, orderAmount = {}"
                , userId, commodityCode, orderTotal, orderAmount);
        return orderService.create(userId, commodityCode, orderTotal, orderAmount);
    }

    @RequestMapping("/count")
    public ObjectResponse<Long> count(){
        log.info("请求订单微服务 >>>> ");

        return orderService.count();
    }
}
