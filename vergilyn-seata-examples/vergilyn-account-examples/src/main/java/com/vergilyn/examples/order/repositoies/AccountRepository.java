package com.vergilyn.examples.order.repositoies;

import com.vergilyn.examples.order.entity.Account;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface AccountRepository extends CrudRepository<Account, Integer> {

    @Modifying
    @Query("update Account set amount = amount - ?2 where userId = ?1")
    int decreaseAccount(String userId, Double amount);

    Account getFirstByUserId(String userId);
}
