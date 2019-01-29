package com.alibaba.fescar.demo.config;

import com.alibaba.fescar.spring.annotation.GlobalTransactionScanner;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FescarConfiguration {

	@Value("${spring.application.name}")
	private String applicationId;

	/**
	 * 注册一个StatViewServlet
	 * 
	 * @return
	 */
	@Bean
	public GlobalTransactionScanner globalTransactionScanner() {
		GlobalTransactionScanner globalTransactionScanner = new GlobalTransactionScanner(applicationId, "receivables");
		return globalTransactionScanner;
	}
}
