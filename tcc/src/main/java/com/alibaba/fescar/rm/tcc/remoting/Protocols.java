package com.alibaba.fescar.rm.tcc.remoting;

import com.alibaba.fescar.common.exception.FrameworkException;

/**
 * remoting protocols enum
 * 
 * @author zhangsen
 *
 */
public enum Protocols {
	
	SOFA_RPC(2),
	
	DUBBO(3),
	
	RESTFUL(4),
	
	IN_JVM(5),
	
	HSF(8)
	;

	private int code;
	
	Protocols(int code){
		this.code = code;
	}

	public int getCode() {
		return code;
	}

	public static Protocols valueOf(int code){
		for(Protocols p : values()){
			if(p.getCode() == code){
				return p;
			}
		}
		throw new FrameworkException("Unknown Protocols, code:" + code);
	}

}
