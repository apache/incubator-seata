package com.alibaba.fescar.account;


import com.alibaba.fescar.account.model.vo.AccountVO;
import com.alibaba.fescar.account.service.IAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "account")
public class AccountController {

    @Autowired
    private IAccountService accountService;

    @RequestMapping(value = "create", method = RequestMethod.POST)
    public String create(@RequestBody AccountVO account) {
        return accountService.create(account);
    }

}
