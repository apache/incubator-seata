package com.vergilyn.examples.dto;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class StorageDTO {
    private Integer id;
    private String commodityCode;
    private String name;
    private Integer total;
}
