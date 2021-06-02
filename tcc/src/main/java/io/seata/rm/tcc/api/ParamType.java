package io.seata.rm.tcc.api;

/**
 * The enum ParamType
 *
 * @author wang.liang
 */
public enum ParamType {

    /**
     * The param
     */
    PARAM("param"),

    /**
     * The field
     */
    FIELD("field");

    private final String code;

    ParamType(String code) {
        this.code = code;
    }

    public String getCode() {
        return this.code;
    }
}
