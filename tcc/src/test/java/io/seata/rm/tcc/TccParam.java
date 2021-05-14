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
package io.seata.rm.tcc;

import io.seata.rm.tcc.api.BusinessActionContextParameter;

/**
 * The type Tcc param.
 *
 * @author zhangsen
 */
public class TccParam {

    /**
     * The Num.
     */
    @BusinessActionContextParameter
    protected int num;

    /**
     * The Name, this field has no annotation
     */
    protected String name;

    /**
     * The Phone.
     */
    @BusinessActionContextParameter(paramName = "phone")
    protected String phone;

    /**
     * The Email.
     */
    @BusinessActionContextParameter(paramName = "email0")
    protected String email;

    /**
     * The Remark.
     */
    @BusinessActionContextParameter(paramName = "phone")
    protected String remark;

    /**
     * Instantiates a new Tcc param.
     *
     * @param num    the num
     * @param name   the name
     * @param phone  the phone
     * @param email  the email
     * @param remark the remark
     */
    public TccParam(int num, String name, String phone, String email, String remark) {
        this.num = num;
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.remark = remark;
    }

    /**
     * Gets num.
     *
     * @return the num
     */
    public int getNum() {
        return num;
    }

    /**
     * Sets num.
     *
     * @param num the num
     */
    public void setNum(int num) {
        this.num = num;
    }
}
