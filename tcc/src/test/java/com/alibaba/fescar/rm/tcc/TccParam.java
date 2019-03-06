package com.alibaba.fescar.rm.tcc;

import com.alibaba.fescar.rm.tcc.api.BusinessActionContextParameter;

/**
 * @author zhangsen
 */
public class TccParam {

    protected int num;

    @BusinessActionContextParameter(paramName = "email")
    protected String email;

    public TccParam(int num, String email) {
        this.num = num;
        this.email = email;
    }

    public int getNum() {
        return num;
    }

    public void setNum(int num) {
        this.num = num;
    }
}
