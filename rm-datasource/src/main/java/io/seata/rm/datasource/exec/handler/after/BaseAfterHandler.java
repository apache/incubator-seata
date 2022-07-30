package io.seata.rm.datasource.exec.handler.after;

import io.seata.rm.datasource.exec.handler.AfterHandler;
import io.seata.rm.datasource.sql.struct.TableRecords;


/**
 * @author: lyx
 */
public abstract class BaseAfterHandler implements AfterHandler {

    @Override
    public String buildAfterSelectSQL(TableRecords beforeImage) {
        return "";
    }
}
