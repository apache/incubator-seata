package com.alibaba.fescar.example.endpoint;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fescar.example.service.*;
import com.alibaba.fescar.example.service.impl.BusinessServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


@RestController
public class ApiEndpoint {

    public static final Logger LOG = LoggerFactory.getLogger(ApiEndpoint.class);

    @Autowired
    BusinessServiceImpl businessService;

    @Autowired
    AccountService accountService;

    @Autowired
    OrderService orderService;

    @Autowired
    StorageService storageService;

    @PostMapping("/api/bussiness/purchase")
    @ResponseBody
    public String purchase(
            @RequestParam("userId") String userId
            , @RequestParam("commodityCode") String commodityCode
            , @RequestParam("orderCount") Integer orderCount
            , @RequestParam("rollback") Boolean rollback
    ) {
        if (rollback != null && rollback == true) {
            businessService.purchaseRollback(userId, commodityCode, orderCount);
        } else {
            businessService.purchase(userId, commodityCode, orderCount);
        }
        return "ok";
    }

    @PostMapping("/api/account/debit")
    @ResponseBody
    public String debit(
            @RequestParam("userId") String userId
            , @RequestParam("money") Integer money) {
        accountService.debit(userId, money);
        return "ok";
    }

    @PostMapping("/api/order/create")
    @ResponseBody
    public JSONObject createOrder(
            @RequestParam("userId") String userId
            , @RequestParam("commodityCode") String commodityCode
            , @RequestParam("count") Integer count) {
        Order order = orderService.create(userId, commodityCode, count);
        return (JSONObject) JSONObject.toJSON(order);
    }

    @PostMapping("/api/storage/deduct")
    @ResponseBody
    public String storageDeduct(
            @RequestParam("commodityCode") String commodityCode
            , @RequestParam("count") Integer count) {
        storageService.deduct(commodityCode, count);
        return "ok";
    }


}
