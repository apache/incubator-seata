package io.seata.config;

/**
 * The enum Config Nacos Data type.
 *
 * @author slievrly
 */
public enum  ConfigNacosDataType {
    /**
     * nacos data type yaml
     */
    yaml,
    /**
     * nacos data type properties
     */
    properties;

    /**
     * Gets type.
     *
     * @param name the name
     * @return the type
     */
    public static ConfigNacosDataType getType(String name) {
        for (ConfigNacosDataType configNacosDataType : values()) {
            if (configNacosDataType.name().equalsIgnoreCase(name)) {
                return configNacosDataType;
            }
        }
        throw new IllegalArgumentException("not support config nacos data type type: " + name);
    }
}
