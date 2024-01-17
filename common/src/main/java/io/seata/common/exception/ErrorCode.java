package io.seata.common.exception;

public enum ErrorCode {

    /**
     * 0001 ~ 0099  Configuration related errors
     */
    ERR_CONFIG(ErrorType.Config, 0001);
    /**
     * The error code of the transaction exception.
     */

    private int code;
    private ErrorType type;

    ErrorCode(ErrorType type, int code) {
        this.code = code;
        this.type = type;
    }

    public int getCode() {
        return code;
    }

    public String getType() {
        return type.name();
    }

    public String getMessage(String... params) {
        return ResourceBundleUtil.getInstance().getMessage(this.name(), this.getCode(), this.getType(), params);
    }

    enum ErrorType {
        Config,
        Network,
        TM,
        RM,
        TC,
        Datasource,
        Other;
    }

}
