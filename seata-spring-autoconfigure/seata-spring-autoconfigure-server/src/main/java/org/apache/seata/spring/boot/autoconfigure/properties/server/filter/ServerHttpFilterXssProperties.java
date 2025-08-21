package org.apache.seata.spring.boot.autoconfigure.properties.server.filter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.apache.seata.common.DefaultValues.DEFAULT_XSS_KEYWORDS;
import static org.apache.seata.spring.boot.autoconfigure.StarterConstants.SERVER_HTTP_FILTER_XSS_PREFIX;

@Component
@ConfigurationProperties(prefix = SERVER_HTTP_FILTER_XSS_PREFIX)
public class ServerHttpFilterXssProperties {
    private boolean enabled = true;

    private List<String> keywords = DEFAULT_XSS_KEYWORDS;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }
}
