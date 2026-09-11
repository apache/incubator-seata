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

import io.netty.buffer.ByteBuf;
import static io.netty.buffer.Unpooled.buffer;

/**
 * RegisterTMRequest Test
 * 
 * @author kaitithoma
 * @author Danaykap
 * 
 *
 */

public class RegisterTMRequestTest {

	private static RegisterTMRequest registerTMRequest;
	private static AbstractIdentifyRequest air;
	private static final String APP_ID = "applicationId";
	private static final String TSG = "transactionServiceGroup";
	private static final String ED = "extraData";
	private static final short TYPE_CODE = 101;
	private static final ByteBuf BB = buffer(128);
	

	
}
