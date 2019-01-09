package com.alibaba.fescar.core.service;

import java.util.List;

/**
 * Watch for server address list change
 */
public interface AddressWatcher {

    void onChange(String[] serverAddressArray);
}
