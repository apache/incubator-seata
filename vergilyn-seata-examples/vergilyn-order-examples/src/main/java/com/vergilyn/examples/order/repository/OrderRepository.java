package com.vergilyn.examples.order.repository;

import com.vergilyn.examples.order.entity.Order;

import org.springframework.data.repository.CrudRepository;

public interface OrderRepository extends CrudRepository<Order, Integer> {
}
