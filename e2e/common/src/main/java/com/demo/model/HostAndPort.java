
package com.demo.model;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * Data model of a host and port.
 */
@Data
@Builder
@Accessors(fluent = true)
public final class HostAndPort {
    private final String host;
    private final int port;

    @Override
    public String toString() {
        return host + ":" + port;
    }
}
