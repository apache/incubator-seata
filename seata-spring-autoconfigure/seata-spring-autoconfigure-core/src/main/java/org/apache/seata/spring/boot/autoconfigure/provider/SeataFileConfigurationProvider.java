package org.apache.seata.spring.boot.autoconfigure.provider;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.apache.seata.common.util.CollectionUtils;
import org.apache.seata.config.FileConfiguration;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.config.file.FileConfig;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;

import static org.apache.seata.common.ConfigurationKeys.FILE_ROOT_PREFIX_CONFIG;
import static org.apache.seata.common.ConfigurationKeys.FILE_ROOT_PREFIX_REGISTRY;
import static org.apache.seata.common.ConfigurationKeys.SERVER_PREFIX;
import static org.apache.seata.common.ConfigurationKeys.STORE_PREFIX;
import static org.apache.seata.common.ConfigurationKeys.METRICS_PREFIX;
import static org.apache.seata.common.ConfigurationKeys.TRANSPORT_PREFIX;
import static org.apache.seata.common.ConfigurationKeys.SEATA_FILE_PREFIX_ROOT_CONFIG;

public class SeataFileConfigurationProvider implements ApplicationContextAware, ApplicationListener<ContextRefreshedEvent> {

    private ApplicationContext applicationContext;

    // Prefix list for filtering configuration keys
    List<String> prefixList = Arrays.asList(FILE_ROOT_PREFIX_CONFIG, FILE_ROOT_PREFIX_REGISTRY, SERVER_PREFIX,
            STORE_PREFIX, METRICS_PREFIX, TRANSPORT_PREFIX);

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        // Only process if the event is for the current application context
        if (event.getApplicationContext().equals(this.applicationContext)) {
            ConfigurableEnvironment environment = (ConfigurableEnvironment) applicationContext.getEnvironment();
            loadAndAddConfigurations(environment);
        }
    }

    /**
     * Loads configurations from file.conf and registry.conf and adds them to the Spring environment.
     *
     * @param environment the Spring environment
     */
    private void loadAndAddConfigurations(ConfigurableEnvironment environment) {
        // Get configurations from file.conf and registry.conf
        FileConfiguration configuration = ConfigurationFactory.getOriginFileInstanceRegistry();
        FileConfig fileConfig = configuration.getFileConfig();
        Map<String, Object> configs = fileConfig.getAllConfig();

        if (CollectionUtils.isNotEmpty(configs)) {
            // Optionally merge other configurations
            Optional<FileConfiguration> originFileInstance = ConfigurationFactory.getOriginFileInstance();
            originFileInstance.ifPresent(fileConfiguration ->
                    configs.putAll(fileConfiguration.getFileConfig().getAllConfig())
            );

            // Convert and filter configurations based on prefix
            Properties properties = new Properties();
            configs.forEach((k, v) -> {
                if (v instanceof String && StringUtils.isNotEmpty((String) v)) {
                    if (prefixList.stream().anyMatch(k::startsWith)) {
                        properties.put(SEATA_FILE_PREFIX_ROOT_CONFIG + k, v);
                    }
                }
            });

            // Add the properties to the environment with the lowest priority
            environment.getPropertySources().addLast(new PropertiesPropertySource("seataFileConfig", properties));
        }
    }
}