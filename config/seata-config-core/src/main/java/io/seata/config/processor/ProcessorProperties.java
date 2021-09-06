package io.seata.config.processor;

import io.seata.common.loader.LoadLevel;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * The properties Processor.
 *
 * @author zhixing
 */
@LoadLevel(name = "properties")
public class ProcessorProperties implements Processor {

    @Override
    public Properties processor(String config) throws IOException {
        Properties properties = new Properties();

        try (Reader reader = new InputStreamReader(new ByteArrayInputStream(config.getBytes()), StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        return properties;
    }
}
