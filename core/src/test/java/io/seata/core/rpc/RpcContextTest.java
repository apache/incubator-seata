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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.*;
import java.util.HashSet;

/**
 * RpcContext Test
 * 
 * @author kaitithoma
 * @author Danaykap
 * 
 * @date 2019/ 3/31
 *
 */

public class RpcContextTest {
	/** RpcContext constructor parameter set as constant **/
	private static RpcContext RC;
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
		RC = new RpcContext();
	}

	/**
	 * Test set ApplicationId to value = "1" Test get ApplicationId
	 */
	@Test
	public void testApplicationIdValue() {
		RC.setApplicationId(ID);
		Assertions.assertEquals(ID, RC.getApplicationId());
	}

	/**
	 * Test set Version to value = "a" Test get Version
	 */
	@Test
	public void testVersionValue() {
		RC.setVersion(VERSION);
		Assertions.assertEquals(VERSION, RC.getVersion());
	}

	/**
	 * Test set ClientId to value = "1" Test get ClientId
	 */
	@Test
	public void testClientIdValue() {
		RC.setClientId(ID);
		Assertions.assertEquals(ID, RC.getClientId());
	}

	/**
	 * Test set Channel to null Test get Channel
	 */
	@Test
	public void testChannelNull() {
		RC.setChannel(null);
		Assertions.assertNull(RC.getChannel());
	}

	/**
	 * Test set TransactionServiceGroup to value = "1" Test get
	 * TransactionServiceGroup
	 */
	@Test
	public void testTransactionServiceGroupValue() {
		RC.setTransactionServiceGroup(TSG);
		Assertions.assertEquals(TSG, RC.getTransactionServiceGroup());
	}

	/**
	 * Test setClientRole to null Test getApplication Id
	 */
	@Test
	public void testClientRoleNull() {
		RC.setClientRole(null);
		Assertions.assertNull(RC.getClientRole());
	}

	/**
	 * Test set ResourceSets to null Test get ResourceSets
	 */
	@Test
	public void testResourceSetsNull() {
		RC.setResourceSets(null);
		Assertions.assertNull(RC.getResourceSets());
	}

	/**
	 * Test add resourceSet = null with addResource Test get ResourceSets
	 */
	@Test
	public void testAddResourceNull() {
		RC.addResource(null);
		HashSet<String> resourceSet = new HashSet<String>();
		resourceSet.add(null);
		Assertions.assertEquals(resourceSet, RC.getResourceSets());
	}

	/**
	 * Test add null parameter to ResourceSets with addResources Test get
	 * ResourceSets
	 */
	@Test
	public void testAddResourcesNull() {
		RC.addResources(null);
		RC.setResourceSets(null);
		Assertions.assertNull(RC.getResourceSets());
	}

	/**
	 * Test add a short resourceSet(["abc"]) with addResources Test get ResourceSets
	 */
	@Test
	public void testAddResourcesResourceValue() {
		HashSet<String> resourceSet = new HashSet<String>();
		resourceSet.add(RV);
		RC.addResources(resourceSet);
		Assertions.assertEquals(resourceSet, RC.getResourceSets());
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
		RC.addResources(resourceSet);
		RC.setResourceSets(resourceSets);
		RC.addResources(resourceSet);
		Assertions.assertEquals(resourceSets, RC.getResourceSets());
	}

	/**
	 * Test toString having all the parameters initialized to null
	 */
	@Test
	public void testToString() {
		RC.setApplicationId(null);
		RC.setTransactionServiceGroup(null);
		RC.setClientId(null);
		RC.setChannel(null);
		RC.setResourceSets(null);
		Assertions.assertEquals(
				"RpcContext{" + "applicationId='" + RC.getApplicationId() + '\'' + ", transactionServiceGroup='"
						+ RC.getTransactionServiceGroup() + '\'' + ", clientId='" + RC.getClientId() + '\''
						+ ", channel=" + RC.getChannel() + ", resourceSets=" + RC.getResourceSets() + '}',
				RC.toString());
	}

}