package com.alibaba.fescar.spring.schema;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.alibaba.fescar.spring.annotation.GlobalTransactionScanner;

public class SchemaTest {

	public static void main(String[]args)throws Exception
	{
		ApplicationContext context = new ClassPathXmlApplicationContext("classpath:/spring-dataSource.xml");
		
		
		Service s1 = (Service) context.getBean("testBean",Service.class);
		System.out.println(s1.payService("cbx"));
		
//		Service s2 = (Service) context.getBean("testBean2",Service.class);
//		System.out.println(s2.pay("baoxiang"));
		
		System.in.read();
	}
}
