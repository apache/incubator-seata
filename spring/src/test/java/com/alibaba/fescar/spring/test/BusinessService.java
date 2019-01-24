package com.alibaba.fescar.spring.test;

import com.alibaba.fescar.core.context.RootContext;
import com.alibaba.fescar.rm.RMClientAT;
import com.alibaba.fescar.spring.annotation.GlobalTransactional;
import com.alibaba.fescar.tm.TMClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

public class BusinessService {

    public static final Logger LOGGER = LoggerFactory.getLogger(BusinessService.class);

    @GlobalTransactional(timeoutMills = 300000, name = "spring-demo-mycat-tx")
    public void purchase(String userId, String commodityCode, int orderCount) {
        String xid = RootContext.getXID();
        doDeduct(xid, commodityCode, orderCount);
        doCreateOrder(xid, userId, commodityCode, orderCount);
    }

    @GlobalTransactional(timeoutMills = 300000, name = "spring-demo-mycat-tx")
    public void purchaseRollback(String userId, String commodityCode, int orderCount) {
        String xid = RootContext.getXID();
        doDeduct(xid, commodityCode, orderCount);
        doCreateOrder(xid, userId, commodityCode, orderCount);
        throw new RollbackException("transaction should be rollback");
    }

    private static Integer doDeduct(final String xid, final String commodityCode, final int count) {
        Callable<Integer> callable = new Callable() {
            @Override
            public Integer call() {
                return BusinessService.deduct(xid, commodityCode, count);
            }
        };
        FutureTask<Integer> task = new FutureTask(callable);
        new ExecThread(task).start();
        try {
            return task.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Order doCreateOrder(final String xid, final String userId, final String commodityCode, final int orderCount) {
        Callable<Order> callable = new Callable() {
            @Override
            public Order call() {
                return BusinessService.createOrder(xid, userId, commodityCode, orderCount);
            }
        };
        FutureTask<Order> task = new FutureTask(callable);
        new ExecThread(task).start();
        try {
            return task.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Integer doDebit(final String xid, final String userId, final int money) {
        Callable<Order> callable = new Callable() {
            @Override
            public Integer call() {
                return BusinessService.debit(xid, userId, money);
            }
        };
        FutureTask<Integer> task = new FutureTask(callable);
        new ExecThread(task).start();
        try {
            return task.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    private static Integer deduct(String xid, String commodityCode, int count) {
        RootContext.bind(xid);

        LOGGER.info("Storage Service Begin ... xid: " + RootContext.getXID());
        LOGGER.info("Deducting inventory SQL: update storage_tbl set count = count - {} where commodity_code = {}", count, commodityCode);

        JdbcTemplate jdbcTemplate = DataSourceFactory.createJdbcTemplate();
        int ret = jdbcTemplate.update("UPDATE storage_tbl SET count = count - ? WHERE commodity_code = ?", new Object[]{count, commodityCode});

        LOGGER.info("Storage Service End ... ");

        return ret;

    }

    private static Order createOrder(String xid, String userId, String commodityCode, int orderCount) {
        RootContext.bind(xid);

        LOGGER.info("Order Service Begin ... xid: " + RootContext.getXID());

        JdbcTemplate jdbcTemplate = DataSourceFactory.createJdbcTemplate();

        // 计算订单金额
        int orderMoney = 200 * orderCount;

        // 从账户余额扣款
        doDebit(xid, userId, orderMoney);

        final Order order = new Order();
        order.userId = userId;
        order.commodityCode = commodityCode;
        order.count = orderCount;
        order.money = orderMoney;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        LOGGER.info("Order Service SQL: insert into order_tbl (user_id, commodity_code, count, money) values ({}, {}, {}, {})", userId, commodityCode, orderCount, orderMoney);

        jdbcTemplate.update(new PreparedStatementCreator() {

            @Override
            public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
                PreparedStatement pst = con.prepareStatement(
                        "INSERT INTO order_tbl (user_id, commodity_code, count, money) VALUES (?, ?, ?, ?)",
                        PreparedStatement.RETURN_GENERATED_KEYS);
                pst.setObject(1, order.userId);
                pst.setObject(2, order.commodityCode);
                pst.setObject(3, order.count);
                pst.setObject(4, order.money);
                return pst;
            }
        }, keyHolder);

        order.id = keyHolder.getKey().longValue();

        LOGGER.info("Order Service End ... Created " + order);

        return order;
    }

    private static Integer debit(String xid, String userId, int money) {
        RootContext.bind(xid);

        LOGGER.info("Account Service Begin ... xid: " + RootContext.getXID());
        LOGGER.info("Deducting balance SQL: update account_tbl set money = money - {} where user_id = {}", money, userId);

        JdbcTemplate jdbcTemplate = DataSourceFactory.createJdbcTemplate();
        Integer ret = jdbcTemplate.update("UPDATE account_tbl SET money = money - ? WHERE user_id = ?", new Object[]{money, userId});
        LOGGER.info("Account Service End ... ");

        return ret;
    }


    static class RollbackException extends RuntimeException {
        public RollbackException(String message) {
            super(message);
        }
    }

    static class ExecThread extends Thread {
        private FutureTask task;

        public ExecThread(FutureTask task) {
            this.task = task;
        }

        @Override
        public void run() {
            String applicationId = this.getName();
            String transactionServiceGroup = "my_test_tx_group";
            TMClient.init(applicationId, transactionServiceGroup);
            RMClientAT.init(applicationId, transactionServiceGroup);

            task.run();
        }
    }


}
