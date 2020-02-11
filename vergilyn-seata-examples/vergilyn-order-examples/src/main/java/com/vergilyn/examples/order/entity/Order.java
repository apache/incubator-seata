package com.vergilyn.examples.order.entity;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "t_order")
@Data
@ToString
@NoArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "order_no")
    private String orderNo;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "commodity_code")
    private String commodityCode;

    @Column(name = "total")
    private Integer total;

    @Column(name = "amount")
    private Double amount;

    public Order(String userId, String commodityCode, Integer total, Double amount) {
        this.orderNo = UUID.randomUUID().toString().replace("-","");
        this.userId = userId;
        this.commodityCode = commodityCode;
        this.total = total;
        this.amount = amount;
    }
}
