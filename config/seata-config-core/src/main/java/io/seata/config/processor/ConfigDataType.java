package io.seata.config.processor;

/**
 * The enum Config Data type.
 *
 * @author zhixing
 */
public enum ConfigDataType {
    /**
     * data type yaml
     */
    yaml,
    /**
     * data type properties
     */
    properties;

    /**
     * Gets type.
     *
     * @param name the name
     * @return the type
     */
    public static ConfigDataType getType(String name) {
        for (ConfigDataType configDataType : values()) {
            if (configDataType.name().equalsIgnoreCase(name)) {
                return configDataType;
            }
        }
        throw new IllegalArgumentException("not support config data type type: " + name);
    }
}
