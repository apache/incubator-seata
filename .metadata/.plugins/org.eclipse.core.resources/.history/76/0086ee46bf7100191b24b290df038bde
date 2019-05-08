package io.seata.core.rpc;

/**
 * RpcContext Test
 * 
 * @author kaitithoma
 * @author Danaykap
 * 
 * @date 2019/ 3/31
 *
 */

import static org.junit.Assert.*;
import org.junit.Test;
import java.util.HashSet;


public class RpcContextTest {

	/**
	 *  RpcContext Constructor 
	*/
	RpcContext rc = new RpcContext();
	
	
	/**
	 * Test set ApplicationId to value = "1"
	 * Test get ApplicationId
	 */
	@Test
	public void testApplicationIdValue() {
		rc.setApplicationId("1");
		assertEquals("1", rc.getApplicationId());
	}
	
	/**
	 * Test set Version to value = "a"
	 * Test get Version
	 */
	@Test
	public void testVersionValue() {
		rc.setVersion("a");
		assertEquals("a", rc.getVersion());
	}
	
	/**
	 * Test set ClientId to value = "1"
	 * Test get ClientId
	 */ 
	@Test
	public void testClientIdValue() {
		rc.setClientId("1");
		assertEquals("1", rc.getClientId());
	}
	
	/**
	 * Test set Channel to null
	 * Test get Channel
	 */
	@Test
	public void testChannelNull() {
		rc.setChannel(null);
		assertNull(rc.getChannel());
	}
	
	/**
	 * Test set TransactionServiceGroup to value = "1"
	 * Test get TransactionServiceGroup
	 */
	@Test
	public void testTransactionServiceGroupValue() {
		rc.setTransactionServiceGroup("b");
		assertEquals("b", rc.getTransactionServiceGroup());
	}
	
	/**
	 * Test setClientRole to null
	 * Test getApplication Id
	 */
	@Test
	public void testClientRoleNull() {
		rc.setClientRole(null);
		assertNull(rc.getClientRole());
	}
	
	/**
	 * Test set ResourceSets to null
	 * Test get ResourceSets
	 */
	@Test
	public void testResourceSetsNull() {
		rc.setResourceSets(null);
		assertNull(rc.getResourceSets());
	}
	
	/**
	 * Test add resourceSet = null with addResource
	 * Test get ResourceSets
	 */
	@Test
	public void testAddResourceNull() {
		rc.addResource(null);
		HashSet<String> resourceSet = new HashSet<String>();
		resourceSet.add(null);
		assertEquals(resourceSet, rc.getResourceSets());
	}
	
	/**
	 * Test set resource parameter to "a" and execute addResource
	 * Test get ResourceSets
	 */
	@Test(expected = Exception.class)
	public void testAddResourceValue() {
		String resource = "a";
		HashSet<String> resourceSet = new HashSet<String>();
		resourceSet.add(resource);
		rc.addResource(resource);
		assertEquals(resourceSet, rc.getResourceSets());
	}
	
	/**
	 * Test add null parameter to ResourceSets with addResources
	 * Test get ResourceSets
	 */
	@Test
	public void testAddResourcesNull() {
		rc.addResources(null);
		rc.setResourceSets(null);
		assertNull(rc.getResourceSets());
	}
	
	/**
	 * Test add a short resourceSet(["abc"]) with addResources
	 * Test get ResourceSets
	 */
	@Test
	public void testAddResourcesResourceValue() {
		HashSet<String> resourceSet = new HashSet<String>();
		resourceSet.add("abc");
		rc.addResources(resourceSet);
		assertEquals(resourceSet, rc.getResourceSets());
	}
	
	/**
	 * Test add resource and resource sets to ResourceSets with addResourceSets
	 * Test getResourceSets
	 */
	@Test
	public void testAddResourcesResourceSetValue() {
		HashSet<String> resourceSets = new HashSet<String>();
		resourceSets.add("a");
		HashSet<String> resourceSet = new HashSet<String>();
		resourceSet.add("abc");
		rc.addResources(resourceSet);
		rc.setResourceSets(resourceSets);
		rc.addResources(resourceSet);
		assertEquals(resourceSets, rc.getResourceSets());
	}
	
	/**
	 * Test toString having all the parameters initialized to null
	 */
	@Test
	public void testToString() {
		rc.setApplicationId(null);
		rc.setTransactionServiceGroup(null);
		rc.setClientId(null);
		rc.setChannel(null);
		rc.setResourceSets(null);
		assertEquals("RpcContext{" +
            "applicationId='" + rc.getApplicationId() + '\'' +
            ", transactionServiceGroup='" +rc.getTransactionServiceGroup() + '\'' +
            ", clientId='" + rc.getClientId() + '\'' +
            ", channel=" + rc.getChannel() +
            ", resourceSets=" + rc.getResourceSets() +
            '}', rc.toString());
	}

}
