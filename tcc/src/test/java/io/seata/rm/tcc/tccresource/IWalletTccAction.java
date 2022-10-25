/*
 * Copyright 1999-2019 Seata.io Group.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package io.seata.rm.tcc.tccresource;

import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.api.BusinessActionContextParameter;
import io.seata.rm.tcc.api.LocalTCC;
import io.seata.rm.tcc.api.TwoPhaseBusinessAction;

import java.math.BigDecimal;
import java.util.List;

/**
 * used test TCCResource and TCCResourceManager
 *
 * @author zouwei
 */
@LocalTCC
public interface IWalletTccAction {

	/**
	 * 预扣款
	 *
	 * @param businessActionContext
	 * @param userId
	 * @param amount
	 * @return
	 */
	@TwoPhaseBusinessAction(name = "prepareDeductMoney", commitMethod = "commitDeductMoney", rollbackMethod = "rollbackDeductMoney")
	boolean prepareDeductMoney(BusinessActionContext businessActionContext,
	                           @BusinessActionContextParameter(paramName = "userId") String userId,
	                           @BusinessActionContextParameter(paramName = "amount") Long amount,
	                           @BusinessActionContextParameter(paramName = "amountF") float amountF,
	                           @BusinessActionContextParameter(paramName = "amountD") Double amountD,
	                           @BusinessActionContextParameter(paramName = "amountB") BigDecimal amountB,
	                           @BusinessActionContextParameter(value = "sublistA", index = 0) List listA,
	                           @BusinessActionContextParameter(value = "sublistB", index = 0) List<String> listB,
	                           @BusinessActionContextParameter(value = "listC") List<String> listC,
	                           @BusinessActionContextParameter(isParamInProperty = true) Hello hello,
	                           @BusinessActionContextParameter(value = "array", index = 0) String[] array
	);


	class Hello {

		@BusinessActionContextParameter("hello")
		private char hello;

		private String name;

		@BusinessActionContextParameter(value = "num", index = 0)
		private List<Integer> nums;

	}

	/**
	 * 提交扣款
	 *
	 * @param businessActionContext
	 * @return
	 */
	boolean commitDeductMoney(BusinessActionContext businessActionContext);

	/**
	 * 回滚扣款
	 *
	 * @param businessActionContext
	 * @return
	 */
	boolean rollbackDeductMoney(BusinessActionContext businessActionContext);
}
