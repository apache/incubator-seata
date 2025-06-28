package org.apache.seata.core.rpc.netty.http.filter;

import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.config.ConfigurationKeys;
import org.apache.seata.core.rpc.netty.http.filter.impl.XSSHttpRequestFilter;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class HttpRequestFilterManagerEnableTest {

    private final Configuration mockConfig = mock(Configuration.class);

    @Test
    void testGlobalEnabledTrueAndXssEnabledTrue() {
        try (MockedStatic<ConfigurationFactory> mockedStatic = mockStatic(ConfigurationFactory.class)) {

            when(ConfigurationFactory.getInstance()).thenReturn(mockConfig);

            when(mockConfig.getBoolean(ConfigurationKeys.SERVER_HTTP_FILTERS_ENABLED, true))
                    .thenReturn(true);
            when(mockConfig.getBoolean(ConfigurationKeys.SERVER_HTTP_FILTER_XSS_ENABLED, true))
                    .thenReturn(true);

            HttpRequestFilterChain filterChain = HttpRequestFilterManager.getFilterChain();
            List<HttpRequestFilter> filters = filterChain.getFilters();

            assertThat(filters).isNotEmpty();
            assertThat(filters.get(0)).isInstanceOf(XSSHttpRequestFilter.class);
        }
    }
}
