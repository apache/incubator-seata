package com.alibaba.fescar.spring.annotation;

public class BusinessImpl implements Business {
    @Override
    @GlobalTransactional(timeoutMills = 300000, name = "busi-doBiz")
    public String doBiz(String msg) {
        System.out.println("Business doBiz");
        return "hello " + msg;
    }
}
