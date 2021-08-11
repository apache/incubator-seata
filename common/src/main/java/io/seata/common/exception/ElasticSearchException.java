package io.seata.common.exception;

/**
 * @author UmizzZ
 * @date
 */
public class ElasticSearchException extends FrameworkException {

    public ElasticSearchException() {
    }

    public ElasticSearchException(FrameworkErrorCode err) {
        super(err);
    }

    public ElasticSearchException(String msg) {
        super(msg);
    }

    public ElasticSearchException(String msg, FrameworkErrorCode errCode) {
        super(msg, errCode);
    }

    public ElasticSearchException(Throwable cause, String msg, FrameworkErrorCode errCode) {
        super(cause, msg, errCode);
    }

    public ElasticSearchException(Throwable th) {
        super(th);
    }

    public ElasticSearchException(Throwable th, String msg) {
        super(th, msg);
    }
}
