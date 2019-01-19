package com.alibaba.fescar.order.feign;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AccountVO {
    private BigDecimal money;
}