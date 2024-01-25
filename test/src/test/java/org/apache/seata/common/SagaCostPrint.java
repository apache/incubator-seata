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
package org.apache.seata.common;

import org.apache.seata.saga.statelang.domain.StateMachineInstance;

/**
 */
public class SagaCostPrint {

	public static StateMachineInstance executeAndPrint(String flag, Executor execute) throws Exception {
		long start = System.nanoTime();

		StateMachineInstance inst = null;
		Exception e = null;
		try {
			inst = execute.run();
		} catch (Exception ex) {
			ex.printStackTrace();
			e = ex;
			throw ex;
		} finally {
			long cost = (System.nanoTime() - start) / 1000_000;
			System.out.printf("====== XID: %s , cost%s: %d ms , error: %s\r\n",
					inst != null ? inst.getId() : null,
					flag,
					cost,
					(e != null ? e.getMessage() : null));
		}
		return inst;
	}

	public static void executeAndPrint(String flag, Runnable runnable) throws Exception {
		executeAndPrint(flag, () -> {
			runnable.run();
			return null;
		});
	}

	@FunctionalInterface
	public interface Executor {
		StateMachineInstance run() throws Exception;
	}

	@FunctionalInterface
	public interface Runnable {
		void run() throws Exception;
	}
}
