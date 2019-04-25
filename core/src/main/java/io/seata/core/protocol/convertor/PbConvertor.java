/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2019 All Rights Reserved.
 */
package io.seata.core.protocol.convertor;

/**
 * @author bystander
 * @version : PbConvertor.java, v 0.1 2019年04月25日 07:43 bystander Exp $
 */
public interface PbConvertor<T, S> {

    public S convert2Proto(T t);

    public T convert2Model(S s);
}