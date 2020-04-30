package io.seata.spring.boot.autoconfigure;

import com.alibaba.druid.util.JdbcUtils;
import io.seata.common.util.StringUtils;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.core.constants.DefaultValues;
import io.seata.spring.boot.autoconfigure.propertysource.DefaultPropertySourceUtils;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Set some default config.
 *
 * @author wangliang <841369634@qq.com>
 */
public class SeataEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private int order = Ordered.LOWEST_PRECEDENCE;

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        this.autoCreateSeataTables(environment);
    }

    /**
     * auto create seata tables.
     *
     * @param env environment
     */
    protected void autoCreateSeataTables(ConfigurableEnvironment env) {
        String initMode = env.getProperty("spring.datasource.initialization-mode");
        if ((initMode != null && !"always".equalsIgnoreCase(initMode)) // not enabled auto execute sql
            || env.containsProperty("spring.datasource.schema") // has custom config
            || env.containsProperty("spring.datasource.schema[0]")) { // has custom config
            return;
        }

        String jdbcUrl = env.getProperty("spring.datasource.url");
        if (StringUtils.isBlank(jdbcUrl)) {
            jdbcUrl = env.getProperty("spring.datasource.jdbc-url");
        }
        String driverClassName = env.getProperty("spring.datasource.driver-class-name");

        String dbType = JdbcUtils.getDbType(jdbcUrl, driverClassName);
        if (dbType == null) {
            return; // unknown db type
        }

        // get default property source
        MapPropertySource defaultPropertySource = DefaultPropertySourceUtils.getDefaultPropertySource(env, true);
        Map<String, Object> defaultProperties = defaultPropertySource.getSource();

        // size of the schema list
        int schemaCount = 0;
        while (defaultProperties.containsKey("spring.datasource.schema[" + schemaCount + "]")) {
            schemaCount++;
        }

        boolean autoCreateTable = false;

        // add at sql file to the schema list
        Boolean enableAtMode = env.getProperty("seata.enable-auto-data-source-proxy", Boolean.class, true);
        String undoLogTable = enableAtMode ? env.getProperty(StarterConstants.SEATA_PREFIX + "." + ConfigurationKeys.TRANSACTION_UNDO_LOG_TABLE) : null;
        if (enableAtMode && (undoLogTable == null || DefaultValues.DEFAULT_TRANSACTION_UNDO_LOG_TABLE.equalsIgnoreCase(undoLogTable))
            && "mysql,oracle,postgresql".contains(dbType)) {
            String atSqlPath = "classpath:sql/seata/at/" + dbType + ".sql";
            defaultProperties.put("spring.datasource.schema[" + schemaCount + "]", atSqlPath);
            schemaCount++;
            autoCreateTable = true;
        }

        // add saga sql file to the schema list
        Boolean enableSagaMode = env.getProperty("seata.saga.enabled", Boolean.class, false);
        String stateMachineTablePrefix = enableSagaMode ? env.getProperty("seata.saga.state-machine.table-prefix") : null;
        if (enableSagaMode && (stateMachineTablePrefix == null || DefaultValues.DEFAULT_STATE_MACHINE_TABLE_PREFIX.equalsIgnoreCase(stateMachineTablePrefix))
            && "db2,h2,mysql,oracle,postgresql".contains(dbType)) {
            String atSqlPath = "classpath:sql/seata/saga/" + dbType + ".sql";
            defaultProperties.put("spring.datasource.schema[" + schemaCount + "]", atSqlPath);
            autoCreateTable = true;
        }

        // enabled auto create table.
        if (autoCreateTable) {
            defaultProperties.put("spring.datasource.initialization-mode", "always");
        }
    }

    /**
     * Get the order value of this object.
     */
    @Override
    public int getOrder() {
        return this.order;
    }
}
