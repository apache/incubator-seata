package com.vergilyn.examples.order.service;

import com.vergilyn.examples.order.entity.Account;
import com.vergilyn.examples.response.ObjectResponse;

public interface AccountService {
    /** 扣用户钱 */
    ObjectResponse<Void> decrease(String userId, Double amount);

    ObjectResponse<Account> get(String userId);
}
