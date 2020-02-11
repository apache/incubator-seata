package com.vergilyn.examples.order.controller;

import com.vergilyn.examples.order.entity.Account;
import com.vergilyn.examples.order.service.AccountService;
import com.vergilyn.examples.response.ObjectResponse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/account")
@Slf4j
public class AccountController {
    @Autowired
    private AccountService accountService;

    @RequestMapping("/decrease")
    ObjectResponse<Void> decrease(@RequestParam("userId") String userId, @RequestParam("amount") Double amount){
        log.info("请求账户微服务 >>>> userId = {}, amount = {}", userId, amount);

        return accountService.decrease(userId, amount);
    }

    @RequestMapping("/get")
    ObjectResponse<Account> get(String userId){
        log.info("请求账户微服务 >>>> userId = {}", userId);
        return accountService.get(userId);
    }

}
