package io.seata.common;

import java.util.Date;

/**
 * The branch do for test
 * @author wangzhongxiang
 */
public class BranchDO {
    private String xid;
    private Long transactionId;
    private Integer status;
    private Double test;
    private Date gmtCreate;

    public String getXid() {
        return xid;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public Integer getStatus() {
        return status;
    }

    public Double getTest() {
        return test;
    }

    public Date getGmtCreate() {
        return gmtCreate;
    }
}
