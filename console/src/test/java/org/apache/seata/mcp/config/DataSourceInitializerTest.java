package org.apache.seata.mcp.config;

import org.apache.seata.mcp.store.DataSourceFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
public class DataSourceInitializerTest {

    @Test
    void testInitInvokesDataSourceFactory() {
        try (MockedStatic<DataSourceFactory> mocked = mockStatic(DataSourceFactory.class)) {
            DataSourceInitializer initializer = new DataSourceInitializer();
            initializer.init();
            mocked.verify(DataSourceFactory::initAllDataSources, times(1));
        }
    }
}
