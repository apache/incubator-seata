package com.alibaba.fescar.rm.tcc.remoting;

/**
 * remoting 服务 bean 描述信息
 * @author zhangsen
 *
 */
public class RemotingDesc {

	/**
	 * 是否是服务订阅bean
	 */
	private boolean isReference = false;
	
	/**
	 * rpc 服务的目标bean, 服务发布bean才有
	 */
	private Object targetBean;
	
	/**
	 * interface 类型
	 */
	private Class<?> interfaceClass ;
	
	/**
	 * interface 类名称
	 */
	private String interfaceClassName ;
	
	/**
	 * rpc uniqueId: hsf、dubbo的version、sofa-rpc的 uniqueId
	 */
	private String uniqueId ;
	
	/**
	 * dubbo的group 分组
	 */
	private String group;
	
	/**
	 * 协议，sofa-rpc、dubbo、restful 等
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
