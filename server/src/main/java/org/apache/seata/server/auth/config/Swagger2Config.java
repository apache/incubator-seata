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
package org.apache.seata.server.auth.config;

import com.google.common.base.Predicates;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.ParameterBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.schema.ModelRef;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Parameter;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;
import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableSwagger2
@EnableWebMvc
public class Swagger2Config {
    @Bean
    public Docket api() {
        ParameterBuilder AccessTokenTicketPar = new ParameterBuilder();
        ParameterBuilder RefreshTokenTicketPar = new ParameterBuilder();
        List<Parameter> pars = new ArrayList<>();
        AccessTokenTicketPar.name("Authorization").description("Access token with prefix \"Bearer\". If no authentication is required, it can be empty")
                .modelRef(new ModelRef("string")).parameterType("header")
                .required(false).build();
        RefreshTokenTicketPar.name("refresh_token").description("Refresh token. If no authentication is required, it can be empty")
                .modelRef(new ModelRef("string")).parameterType("header")
                .required(false).build();

        pars.add(AccessTokenTicketPar.build());
        pars.add(RefreshTokenTicketPar.build());

        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
                .useDefaultResponseMessages(true)
                .forCodeGeneration(false)
                .select()
                .apis(Predicates.or(
                        RequestHandlerSelectors.basePackage("org.apache.seata.server"),
                        RequestHandlerSelectors.basePackage("org.apache.seata.console")
                ))
                .paths(PathSelectors.any())
                .build()
                .globalOperationParameters(pars);
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("seata API")
                .description("Learn more about seata: https://github.com/apache/incubator-seata")
                .build();
    }
}
