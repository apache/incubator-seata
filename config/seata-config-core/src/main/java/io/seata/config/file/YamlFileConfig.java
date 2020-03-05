package io.seata.config.file;

import java.io.File;

public class YamlFileConfig implements FileConfig{


    public YamlFileConfig(String filePath){

    }

    public YamlFileConfig(File file){

    }

    @Override
    public String getString(String path) {
        return null;
    }
}
