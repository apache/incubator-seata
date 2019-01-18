package com.alibaba.fescar.order.model.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * @author Loki
 */
@Data
public class OrderVO {
    private String orderId;
    private BigDecimal money;
    private String commodityCode;
}
