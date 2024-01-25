/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.integration.tx.api.remoting;

/**
 * remoting bean info
 *
 */
public class RemotingDesc {

    /**
     * is referenc bean ?
     */
    private boolean isReference = false;

    /**
     * is service bean ?
     */
    private boolean isService = false;

    /**
     * rpc target bean, the service bean has this property
     */
    private Object targetBean;

    /**
     * the tcc serviceClass type
     */
    private Class<?> serviceClass;

    /**
     * serviceClass name
     */
    private String serviceClassName;

    /**
     * rpc uniqueId: hsf, dubbo's version, sofa-rpc's uniqueId
     */
    private String uniqueId;

    /**
     * dubbo/hsf 's group
     */
    private String group;

    /**
     * protocol: sofa-rpc, dubbo, injvm etc.
     */
    private short protocol;

    /**
     * Gets target bean.
     *
     * @return the target bean
     */
    public Object getTargetBean() {
        return targetBean;
    }

    /**
     * Sets target bean.
     *
     * @param targetBean the target bean
     */
    public void setTargetBean(Object targetBean) {
        this.targetBean = targetBean;
    }

    /**
     * Gets serviceClass.
     *
     * @return the serviceClass
     */
    public Class<?> getServiceClass() {
        return serviceClass;
    }

    /**
     * Sets serviceClass.
     *
     * @param serviceClass the serviceClass
     */
    public void setServiceClass(Class<?> serviceClass) {
        this.serviceClass = serviceClass;
    }

    /**
     * Gets serviceClass name.
     *
     * @return the serviceClass name
     */
    public String getServiceClassName() {
        return serviceClassName;
    }

    /**
     * Sets serviceClass name.
     *
     * @param serviceClassName the serviceClass name
     */
    public void setServiceClassName(String serviceClassName) {
        this.serviceClassName = serviceClassName;
    }

    /**
     * Gets unique id.
     *
     * @return the unique id
     */
    public String getUniqueId() {
        return uniqueId;
    }

    /**
     * Sets unique id.
     *
     * @param uniqueId the unique id
     */
    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    /**
     * Gets group.
     *
     * @return the group
     */
    public String getGroup() {
        return group;
    }

    /**
     * Sets group.
     *
     * @param group the group
     */
    public void setGroup(String group) {
        this.group = group;
    }

    /**
     * Gets protocol.
     *
     * @return the protocol
     */
    public short getProtocol() {
        return protocol;
    }

    /**
     * Sets protocol.
     *
     * @param protocol the protocol
     */
    public void setProtocol(short protocol) {
        this.protocol = protocol;
    }

    /**
     * Is reference boolean.
     *
     * @return the boolean
     */
    public boolean isReference() {
        return isReference;
    }

    /**
     * Sets reference.
     *
     * @param reference the reference
     */
    public void setReference(boolean reference) {
        isReference = reference;
    }

    /**
     * Is service boolean.
     *
     * @return the boolean
     */
    public boolean isService() {
        return isService;
    }

    /**
     * Sets service.
     * @param service the service
     */
    public void setService(boolean service) {
        isService = service;
    }
}
