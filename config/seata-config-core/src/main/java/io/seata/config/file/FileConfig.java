package io.seata.config.file;

import com.typesafe.config.ConfigException;

public interface FileConfig {
    /**
     * @param path
     *            path expression
     */
    String getString(String path);


}
