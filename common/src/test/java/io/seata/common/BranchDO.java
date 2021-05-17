/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

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

    public BranchDO() {
    }

    public BranchDO(String xid, Long transactionId, Integer status, Double test,
            Date gmtCreate) {
        this.xid = xid;
        this.transactionId = transactionId;
        this.status = status;
        this.test = test;
        this.gmtCreate = gmtCreate;
    }
}
