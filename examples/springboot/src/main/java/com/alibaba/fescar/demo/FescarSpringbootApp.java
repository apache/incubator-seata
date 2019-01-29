package com.alibaba.fescar.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.alibaba.dubbo.spring.boot.annotation.EnableDubboConfiguration;
import com.alibaba.fescar.demo.service.AssignService;

/**
 * @Description
 * @author 张国豪
 */
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
@EnableDubboConfiguration
@EnableTransactionManagement
@ComponentScan({ "com.alibaba.fescar.demo" })
public class FescarSpringbootApp {
	private static final Logger LOGGER = LoggerFactory.getLogger(FescarSpringbootApp.class);

	public static void main(String[] args) {
		LOGGER.debug("springboot project with fescar starting...");
		ConfigurableApplicationContext context = new SpringApplication(FescarSpringbootApp.class).run(args);

		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			LOGGER.error("error when sleep", e);
		}
		AssignService assignService = context.getBean(AssignService.class);
		assignService.increaseAmount("14070e0e3cfe403098fa9ca37e8e7e76");
	}

}
