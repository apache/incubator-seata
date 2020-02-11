package com.vergilyn.examples.storage.entity;

import java.io.Serializable;

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
@Table(name = "t_storage")
@Data
@NoArgsConstructor
@ToString
public class Storage implements Serializable{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "commodity_code")
    private String commodityCode;

    @Column(name = "name")
    private String name;

    @Column(name = "total")
    private Integer total;

}
