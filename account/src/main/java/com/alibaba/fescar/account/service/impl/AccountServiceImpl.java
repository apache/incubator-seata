package com.alibaba.fescar.account.service.impl;

import com.alibaba.fescar.account.mapper.AccountDOMapper;
import com.alibaba.fescar.account.model.dao.AccountDO;
import com.alibaba.fescar.account.model.vo.AccountVO;
import com.alibaba.fescar.account.service.IAccountService;
import com.alibaba.fescar.core.context.RootContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AccountServiceImpl implements IAccountService {

    @Autowired
    AccountDOMapper accountDOMapper;

    @Override
    public String create(AccountVO account) {
        log.debug("xid={}", RootContext.getXID());
        AccountDO accountDO = new AccountDO();
        accountDO.setMoney(account.getMoney());

        int result = accountDOMapper.insert(accountDO);

        return String.valueOf(result);
    }
}
