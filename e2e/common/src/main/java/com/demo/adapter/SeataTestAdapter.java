package com.demo.adapter;

import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;

/**
 * Integrate some thing commonly used in scene test.
 */
public abstract class SeataTestAdapter {

    protected final RestTemplate restTemplate = new RestTemplate();
    protected TrafficController requestController;
    protected TimesController timesController;
    protected DruidJdbcQuery druidJdbcQuery;


    public void druidJdbcQuery(Map map) {
        try {
            druidJdbcQuery = new DruidJdbcQuery(map);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void druidJdbcQuery(Properties pro) {
        try {
            druidJdbcQuery = new DruidJdbcQuery(pro);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void trafficController(Callable<?> sender) throws Exception {
        requestController =
                TrafficController.builder()
                        .sender(sender)
                        .build()
                        .start();

    }

    public void timseController(Callable<?> sender, int times) throws Exception {

        timesController =
                TimesController.builder()
                        .sender(sender)
                        .build()
                        .start(times);

    }


}
