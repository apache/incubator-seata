package io.seata.config.file;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.seata.common.loader.LoadLevel;

import java.io.File;

@LoadLevel(name = "CONF")
public class SimpleFileConfig implements FileConfig {

    private Config fileConfig;

    public SimpleFileConfig(String filePath) {
        fileConfig = ConfigFactory.load(filePath);
    }

    public SimpleFileConfig() {
        fileConfig = ConfigFactory.load();
    }

    public SimpleFileConfig(File file) {
        Config appConfig = ConfigFactory.parseFileAnySyntax(file);
        fileConfig = ConfigFactory.load(appConfig);
    }

    @Override
    public String getString(String path) {
        return fileConfig.getString(path);
    }
}
