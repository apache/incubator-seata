package com.alibaba.fescar.server.session;

/**
 * Service contains states which can be reloaded.
 */
public interface Reloadable {

    /**
     * Reload states.
     */
    void reload();
}
