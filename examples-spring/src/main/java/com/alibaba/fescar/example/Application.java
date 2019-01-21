package com.alibaba.fescar.example;

import com.alibaba.fescar.example.config.EnvUtil;
import com.alibaba.fescar.example.config.SpringContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.dao.PersistenceExceptionTranslationAutoConfiguration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;


@SpringBootApplication(
        exclude = {PersistenceExceptionTranslationAutoConfiguration.class}
)
public class Application {
    public static final Logger LOG = LoggerFactory.getLogger(Application.class);

    /**
     * 服务启动
     */
    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(Application.class);
        application.run(args);

        //application启动后,执行初始化操作
        List<String> sqls = EnvUtil.getListSubProperty("app.init-sql");
        JdbcTemplate jdbcTemplate = SpringContextUtil.getBean(JdbcTemplate.class);

        for (String sql : sqls) {
            jdbcTemplate.execute(sql);
            LOG.info("execute init sql:{}", sql);
        }

        LOG.info("SpringBoot Start Success");
    }
}
