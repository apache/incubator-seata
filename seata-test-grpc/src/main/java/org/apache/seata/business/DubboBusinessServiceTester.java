/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.business;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.apache.seata.business.service.BusinessService;
import org.apache.seata.spring.annotation.datasource.EnableAutoDataSourceProxy;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

@EnableAutoDataSourceProxy
@EnableDubbo
@ComponentScan(basePackages = {"org.apache.seata.business"})
public class DubboBusinessServiceTester {

    /**
     * Enable PropertySource placeHolder
     */
    @Bean
    public PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    public static void main(String[] args) throws InterruptedException {
        AnnotationConfigApplicationContext annotationConfigApplicationContext = new AnnotationConfigApplicationContext(DubboBusinessServiceTester.class);

        BusinessService businessService = annotationConfigApplicationContext.getBean(BusinessService.class);
        Thread thread = new Thread(() -> businessService.purchase("U100001", "C00321", 2));
        thread.start();

        //keep run
        Thread.currentThread().join();
    }

}
