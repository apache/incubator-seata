package io.seata.server.starter.controller;

import io.seata.server.starter.SeataServerRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;


@Controller
@RequestMapping
public class HealthController {
    private Logger logger = LoggerFactory.getLogger(HealthController.class);

    private static final String OK = "ok";
    private static final String NOT_OK = "not_ok";

    @Autowired
    private SeataServerRunner seataServerRunner;


    @RequestMapping("/health")
    @ResponseBody
    String healthCheck() {
        boolean started = seataServerRunner.started();
        String result = started ? OK : NOT_OK;
        return result;
    }
}