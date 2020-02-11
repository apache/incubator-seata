package com.vergilyn.examples.dto;

import java.io.Serializable;
import java.math.BigDecimal;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class AccountDTO implements Serializable {

    private Integer id;

    private String userId;

    private BigDecimal amount;
}
