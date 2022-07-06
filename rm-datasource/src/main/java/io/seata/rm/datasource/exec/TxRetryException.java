package io.seata.rm.datasource.exec;

import java.sql.SQLException;

/**
 * @author jianbin.chen
 */
public class TxRetryException extends SQLException {

	public TxRetryException(String reason, String SQLState, int vendorCode) {
		super(reason, SQLState, vendorCode);
	}

	public TxRetryException(String reason, String SQLState) {
		super(reason, SQLState);
	}

	public TxRetryException(String reason) {
		super(reason);
	}

	public TxRetryException() {
	}

	public TxRetryException(Throwable cause) {
		super(cause);
	}

	public TxRetryException(String reason, Throwable cause) {
		super(reason, cause);
	}

	public TxRetryException(String reason, String sqlState, Throwable cause) {
		super(reason, sqlState, cause);
	}

	public TxRetryException(String reason, String sqlState, int vendorCode, Throwable cause) {
		super(reason, sqlState, vendorCode, cause);
	}
}
