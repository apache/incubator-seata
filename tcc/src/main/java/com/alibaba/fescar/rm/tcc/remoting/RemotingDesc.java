/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
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
package com.alibaba.fescar.rm.tcc.remoting;

/**
 * remoting bean info
 * @author zhangsen
 *
 */
public class RemotingDesc {

	/**
	 * is referenc bean ?
	 */
	private boolean isReference = false;
	
	/**
	 * rpc target bean, the service bean has this property
	 */
	private Object targetBean;
	
	/**
	 * the tcc interface tyep
	 */
	private Class<?> interfaceClass ;
	
	/**
	 * interface class name
	 */
	private String interfaceClassName ;
	
	/**
	 * rpc uniqueId: hsf、dubbo的version、sofa-rpc 的 uniqueId
	 */
	private String uniqueId ;
	
	/**
	 * dubbo/hsf 的group 分组
	 */
	private String group;
	
	/**
	 * protocol: sofa-rpc、dubbo、injvm 等
	 */
	private int protocol;
	
	public Object getTargetBean() {
		return targetBean;
	}

	public void setTargetBean(Object targetBean) {
		this.targetBean = targetBean;
	}

	public Class<?> getInterfaceClass() {
		return interfaceClass;
	}

	public void setInterfaceClass(Class<?> interfaceClass) {
		this.interfaceClass = interfaceClass;
	}

	public String getInterfaceClassName() {
		return interfaceClassName;
	}

	public void setInterfaceClassName(String interfaceClassName) {
		this.interfaceClassName = interfaceClassName;
	}

	public String getUniqueId() {
		return uniqueId;
	}

	public void setUniqueId(String uniqueId) {
		this.uniqueId = uniqueId;
	}

	public String getGroup() {
		return group;
	}

	public void setGroup(String group) {
		this.group = group;
	}

	public int getProtocol() {
		return protocol;
	}

	public void setProtocol(int protocol) {
		this.protocol = protocol;
	}

	public boolean isReference() {
		return isReference;
	}

	public void setReference(boolean reference) {
		isReference = reference;
	}
}
