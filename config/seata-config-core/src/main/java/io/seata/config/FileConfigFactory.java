package io.seata.config;

import com.sun.scenario.effect.impl.prism.PrImage;
import com.typesafe.config.ConfigFactory;
import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.common.loader.EnhancedServiceNotFoundException;
import io.seata.config.file.FileConfig;
import io.seata.config.file.SimpleFileConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class FileConfigFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileConfigFactory.class);

    private static final String DEFAULT_TYPE = "CONF";

    private static final Map<String,String> SUFFIX_MAP = new HashMap<String,String>(4){{
        put("conf","CONF");
        put("properties","CONF");
        put("yml","YAML");
    }};

    public static FileConfig load(String filePath) {
        String configType = getConfigType(filePath);
        return loadService(configType, new Class[]{String.class}, new Object[]{filePath});
    }

    public static FileConfig load() {

        return loadService(DEFAULT_TYPE, null, null);
    }

    public static FileConfig load(File targetFile) {
        String fileName = targetFile.getName();
        String configType = getConfigType(fileName);
        return loadService(configType, new Class[]{File.class}, new Object[]{targetFile});
    }

    private static String getConfigType(String fileName) {
        String configType = DEFAULT_TYPE;
        int suffixIndex = fileName.lastIndexOf(".");
        if (suffixIndex > 0) {
            configType = fileName.substring(suffixIndex);
        }
        if (configType == null || configType.length() == 0) {
            configType = DEFAULT_TYPE;
        }
        return configType;
    }

    private static FileConfig loadService(String name, Class[] argsType, Object[] args) {
        FileConfig fileConfig = EnhancedServiceLoader.load(FileConfig.class, name, argsType, args);
        return fileConfig;
    }



}
