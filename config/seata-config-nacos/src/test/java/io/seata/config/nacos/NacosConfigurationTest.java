package io.seata.config.nacos;

import org.apache.http.util.Asserts;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.InputStreamResource;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class NacosConfigurationTest {
    @Test
    public void loadYaml(){
        String yaml ="store:\n" +
                "  mode: db\n" +
                "  db: \n" +
                "    datasource: druid\n" +
                "    dbType: mysql\n" +
                "    driverClassName: com.mysql.jdbc.Driver\n" +
                "    url: jdbc:mysql://127.0.0.1:3306/server_seata\n" +
                "    user: root\n" +
                "    password: 'root'\n";
        final YamlPropertiesFactoryBean yamlPropertiesFactoryBean = new YamlPropertiesFactoryBean();
        final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
        final InputStreamResource inputStreamResource = new InputStreamResource(byteArrayInputStream);
        yamlPropertiesFactoryBean.setResources(inputStreamResource);
        final Properties object = yamlPropertiesFactoryBean.getObject();
        System.out.println(object);
        Asserts.notNull(object,"loadYaml");
    }
}