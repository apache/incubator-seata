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
package io.seata.core.protocol;

import io.seata.core.protocol.transaction.BranchCommitRequest;
import io.seata.core.protocol.transaction.BranchCommitResponse;
import io.seata.core.protocol.transaction.BranchRegisterRequest;
import io.seata.core.protocol.transaction.BranchRegisterResponse;
import io.seata.core.protocol.transaction.BranchReportRequest;
import io.seata.core.protocol.transaction.BranchReportResponse;
import io.seata.core.protocol.transaction.BranchRollbackRequest;
import io.seata.core.protocol.transaction.BranchRollbackResponse;
import io.seata.core.protocol.transaction.GlobalBeginRequest;
import io.seata.core.protocol.transaction.GlobalBeginResponse;
import io.seata.core.protocol.transaction.GlobalCommitRequest;
import io.seata.core.protocol.transaction.GlobalCommitResponse;
import io.seata.core.protocol.transaction.GlobalLockQueryRequest;
import io.seata.core.protocol.transaction.GlobalLockQueryResponse;
import io.seata.core.protocol.transaction.GlobalRollbackRequest;
import io.seata.core.protocol.transaction.GlobalRollbackResponse;
import io.seata.core.protocol.transaction.GlobalStatusRequest;
import io.seata.core.protocol.transaction.GlobalStatusResponse;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class AbstractMessageTest {
	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	@Test
	public void testBytesToInt() {
		Assert.assertEquals(0, AbstractMessage.bytesToInt(new byte[] {0, 0}, 1));
		Assert.assertEquals(0,
			AbstractMessage.bytesToInt(new byte[] { 0, 0, 0, 0 }, 0));
		Assert.assertEquals(0,
			AbstractMessage.bytesToInt(new byte[] { 0, 0, 1, 1, 1 }, 524_288));
	}

	@Test
	public void testIntToBytes() {
		AbstractMessage.intToBytes(0, new byte[] { 0, 0, 0, 0 }, 0);
	}

	@Test
	public void testGetMsgInstanceByCode() {
		Assert.assertEquals(new MergedWarpMessage().getClass(),
			AbstractMessage.getMsgInstanceByCode((short) 59).getClass());
		Assert.assertEquals(new MergeResultMessage().getClass(),
			AbstractMessage.getMsgInstanceByCode((short) 60).getClass());
		Assert.assertEquals(new RegisterTMRequest().getClass(),
			AbstractMessage.getMsgInstanceByCode((short) 101).getClass());
		Assert.assertEquals(new RegisterTMResponse().getClass(),
			AbstractMessage.getMsgInstanceByCode((short) 102).getClass());
		Assert.assertEquals(new RegisterRMRequest().getClass(),
			AbstractMessage.getMsgInstanceByCode((short) 103).getClass());
		Assert.assertEquals(new RegisterRMResponse().getClass(),
			AbstractMessage.getMsgInstanceByCode((short) 104).getClass());
		Assert.assertEquals(new BranchCommitRequest().getClass(),
			AbstractMessage.getMsgInstanceByCode((short) 3).getClass());
		Assert.assertEquals(new BranchRollbackRequest().getClass(),
			AbstractMessage.getMsgInstanceByCode((short) 5).getClass());

		Assert.assertEquals(new GlobalBeginRequest().getClass(),
			AbstractMessage.getMsgInstanceByCode((short) 1).getClass());
		Assert.assertEquals(new GlobalBeginResponse().getClass(),
			AbstractMessage.getMsgInstanceByCode((short) 2).getClass());
	}

	@Test
	public void testGetMergeRequestInstanceByCode() {
		Assert.assertEquals(new GlobalBeginRequest().getClass(),
			AbstractMessage.getMergeRequestInstanceByCode(1).getClass());
		Assert.assertEquals(new GlobalCommitRequest().getClass(),
			AbstractMessage.getMergeRequestInstanceByCode(7).getClass());
		Assert.assertEquals(new GlobalRollbackRequest().getClass(),
			AbstractMessage.getMergeRequestInstanceByCode(9).getClass());
		Assert.assertEquals(new GlobalStatusRequest().getClass(),
			AbstractMessage.getMergeRequestInstanceByCode(15).getClass());
		Assert.assertEquals(new GlobalLockQueryRequest().getClass(),
			AbstractMessage.getMergeRequestInstanceByCode(21).getClass());
		Assert.assertEquals(new BranchRegisterRequest().getClass(),
			AbstractMessage.getMergeRequestInstanceByCode(11).getClass());
		Assert.assertEquals(new BranchReportRequest().getClass(),
			AbstractMessage.getMergeRequestInstanceByCode(13).getClass());

		thrown.expect(IllegalArgumentException.class);
		AbstractMessage.getMergeRequestInstanceByCode(536_870_912);
	}

	@Test
	public void testGetMergeResponseInstanceByCode() {
		Assert.assertEquals(new GlobalBeginResponse().getClass(),
			AbstractMessage.getMergeResponseInstanceByCode(2).getClass());
		Assert.assertEquals(new GlobalCommitResponse().getClass(),
			AbstractMessage.getMergeResponseInstanceByCode(8).getClass());
		Assert.assertEquals(new GlobalRollbackResponse().getClass(),
			AbstractMessage.getMergeResponseInstanceByCode(10).getClass());
		Assert.assertEquals(new GlobalStatusResponse().getClass(),
			AbstractMessage.getMergeResponseInstanceByCode(16).getClass());
		Assert.assertEquals(new GlobalLockQueryResponse().getClass(),
			AbstractMessage.getMergeResponseInstanceByCode(22).getClass());
		Assert.assertEquals(new BranchRegisterResponse().getClass(),
			AbstractMessage.getMergeResponseInstanceByCode(12).getClass());
		Assert.assertEquals(new BranchReportResponse().getClass(),
			AbstractMessage.getMergeResponseInstanceByCode(14).getClass());
		Assert.assertEquals(new BranchCommitResponse().getClass(),
			AbstractMessage.getMergeResponseInstanceByCode(4).getClass());
		Assert.assertEquals(new BranchRollbackResponse().getClass(),
			AbstractMessage.getMergeResponseInstanceByCode(6).getClass());

		thrown.expect(IllegalArgumentException.class);
		Assert.assertEquals(new BranchRollbackResponse().getClass(),
			AbstractMessage.getMergeResponseInstanceByCode(0).getClass());
	}
}
