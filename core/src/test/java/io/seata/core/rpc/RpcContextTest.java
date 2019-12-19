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
package io.seata.core.rpc;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.util.HashSet;

/**
 * RpcContext Test
 * 
 * @author kaitithoma
 * @author Danaykap
 * 
 *
 */

public class RpcContextTest {
	/** RpcContext constructor parameter set as constant **/
	private static RpcContext rpcContext;
	/** Version value **/
	private static final String VERSION = "a";
	/** TransactionServiceGroup value **/
	private static final String TSG = "a"; 
	/** ID value for every method needing an Id **/
	private static final String ID = "1"; 
	/** ResourceValue value **/
	private static final String RV = "abc";
	/** ResourceSet value **/
	private static final String RS = "b"; 

	/**
	 * RpcContext Constructor
	 */

	@BeforeAll
	public static void setup() {
		rpcContext = new RpcContext();
	}

	/**
	 * Test set ApplicationId to value = "1" Test get ApplicationId
	 */
	@Test
	public void testApplicationIdValue() {
		rpcContext.setApplicationId(ID);
		Assertions.assertEquals(ID, rpcContext.getApplicationId());
	}

	/**
	 * Test set Version to value = "a" Test get Version
	 */
	@Test
	public void testVersionValue() {
		rpcContext.setVersion(VERSION);
		Assertions.assertEquals(VERSION, rpcContext.getVersion());
	}

	/**
	 * Test set ClientId to value = "1" Test get ClientId
	 */
	@Test
	public void testClientIdValue() {
		rpcContext.setClientId(ID);
		Assertions.assertEquals(ID, rpcContext.getClientId());
	}

	/**
	 * Test set Channel to null Test get Channel
	 */
	@Test
	public void testChannelNull() {
		rpcContext.setChannel(null);
		Assertions.assertNull(rpcContext.getChannel());
	}

	/**
	 * Test set TransactionServiceGroup to value = "1" Test get
	 * TransactionServiceGroup
	 */
	@Test
	public void testTransactionServiceGroupValue() {
		rpcContext.setTransactionServiceGroup(TSG);
		Assertions.assertEquals(TSG, rpcContext.getTransactionServiceGroup());
	}

	/**
	 * Test setClientRole to null Test getApplication Id
	 */
	@Test
	public void testClientRoleNull() {
		rpcContext.setClientRole(null);
		Assertions.assertNull(rpcContext.getClientRole());
	}

	/**
	 * Test set ResourceSets to null Test get ResourceSets
	 */
	@Test
	public void testResourceSetsNull() {
		rpcContext.setResourceSets(null);
		Assertions.assertNull(rpcContext.getResourceSets());
	}

	/**
	 * Test add resourceSet = null with addResource Test get ResourceSets
	 */
	@Test
	public void testAddResourceNull() {
		rpcContext.addResource(null);
		HashSet<String> resourceSet = new HashSet<String>();
		resourceSet.add(null);
		Assertions.assertEquals(resourceSet, rpcContext.getResourceSets());
	}

	/**
	 * Test add null parameter to ResourceSets with addResources Test get
	 * ResourceSets
	 */
	@Test
	public void testAddResourcesNull() {
		rpcContext.addResources(null);
		rpcContext.setResourceSets(null);
		Assertions.assertNull(rpcContext.getResourceSets());
	}

	/**
	 * Test add a short resourceSet(["abc"]) with addResources Test get ResourceSets
	 */
	@Test
	public void testAddResourcesResourceValue() {
		HashSet<String> resourceSet = new HashSet<String>();
		resourceSet.add(RV);
		rpcContext.addResources(resourceSet);
		Assertions.assertEquals(resourceSet, rpcContext.getResourceSets());
	}

	/**
	 * Test add resource and resource sets to ResourceSets with addResourceSets Test
	 * getResourceSets
	 */
	@Test
	public void testAddResourcesResourceSetValue() {
		HashSet<String> resourceSets = new HashSet<String>();
		resourceSets.add(RS);
		HashSet<String> resourceSet = new HashSet<String>();
		resourceSet.add(RV);
		rpcContext.addResources(resourceSet);
		rpcContext.setResourceSets(resourceSets);
		rpcContext.addResources(resourceSet);
		Assertions.assertEquals(resourceSets, rpcContext.getResourceSets());
	}

	/**
	 * Test toString having all the parameters initialized to null
	 */
	@Test
	public void testToString() {
		rpcContext.setApplicationId(null);
		rpcContext.setTransactionServiceGroup(null);
		rpcContext.setClientId(null);
		rpcContext.setChannel(null);
		rpcContext.setResourceSets(null);
		Assertions.assertEquals(
				"RpcContext{" + "applicationId='" + rpcContext.getApplicationId() + '\'' + ", transactionServiceGroup='"
						+ rpcContext.getTransactionServiceGroup() + '\'' + ", clientId='" + rpcContext.getClientId() + '\''
						+ ", channel=" + rpcContext.getChannel() + ", resourceSets=" + rpcContext.getResourceSets() + '}',
						rpcContext.toString());
	}

}
