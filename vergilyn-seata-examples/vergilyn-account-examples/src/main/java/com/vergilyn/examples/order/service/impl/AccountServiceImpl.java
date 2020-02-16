package com.vergilyn.examples.order.service.impl;

import javax.transaction.Transactional;

import com.vergilyn.examples.order.entity.Account;
import com.vergilyn.examples.order.repositoies.AccountRepository;
import com.vergilyn.examples.order.service.AccountService;
import com.vergilyn.examples.response.ObjectResponse;

import io.seata.core.context.RootContext;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AccountServiceImpl implements AccountService {
    @Autowired
    private AccountRepository accountRepository;

    @Override
    @Transactional
    @GlobalTransactional(name = "vergilyn-first-global-transaction", rollbackFor = Exception.class)
    public ObjectResponse<Void> decrease(String userId, Double amount) {
        log.info("开启全局事务 >>>> xid: {}", RootContext.getXID());
        int account = accountRepository.decreaseAccount(userId, amount);

        return account > 0 ? ObjectResponse.success() : ObjectResponse.failure();
    }

    @Override
    public ObjectResponse<Account> get(String userId) {
        Account account = accountRepository.getFirstByUserId(userId);
        return ObjectResponse.success(account);
    }
}
