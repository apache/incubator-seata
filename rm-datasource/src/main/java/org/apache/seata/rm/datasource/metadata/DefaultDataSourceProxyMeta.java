package org.apache.seata.rm.datasource.metadata;

import javax.sql.DataSource;
import java.sql.SQLException;

public class DefaultDataSourceProxyMeta extends AbstractDataSourceProxyMetadata {

    @Override
    public void init(DataSource dataSource) throws SQLException {
        super.init(dataSource);
    }

    @Override
    public String getVariableValue(String name) {
        return null;
    }

    @Override
    public String getKernelVersion() {
        return null;
    }

}
