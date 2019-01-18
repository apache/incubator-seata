package com.alibaba.fescar.order.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * @author Loki
 */
@Configuration
@EnableSwagger2
@ConditionalOnProperty(name = "swagger.enabled", matchIfMissing = true)
public class SwaggerConfig {
}
