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
package io.seata.codec.seata.protocol.transaction;

import io.seata.core.model.BranchType;
import io.seata.core.protocol.transaction.UndoLogDeleteRequest;

import java.nio.ByteBuffer;

/**
 * The type UndoLog Delete end request codec.
 *
 * @author yuanguoyao
 */
public class UndoLogDeleteRequestCodec extends AbstractTransactionRequestToRMCodec {

    @Override
    public Class<?> getMessageClassType() {
        return UndoLogDeleteRequest.class;
    }

    @Override
    public <T> void encode(T t, ByteBuffer out){
        UndoLogDeleteRequest undoLogDeleteRequest = (UndoLogDeleteRequest) t;
        short saveDays =  undoLogDeleteRequest.getSaveDays();
        BranchType branchType = undoLogDeleteRequest.getBranchType();
        String resourceId = undoLogDeleteRequest.getResourceId();

        // 1. Branch Type
        out.put((byte)branchType.ordinal());

        // 2. Resource Id
        if (resourceId != null) {
            byte[] bs = resourceId.getBytes(UTF8);
            out.putShort((short)bs.length);
            if (bs.length > 0) {
                out.put(bs);
            }
        } else {
            out.putShort((short)0);
        }

        //3.save days
        out.putShort(saveDays);
    }

    @Override
    public <T> void decode(T t, ByteBuffer in) {
        UndoLogDeleteRequest undoLogDeleteRequest = (UndoLogDeleteRequest) t;

        if (in.remaining() < 1) {
            return ;
        }
        undoLogDeleteRequest.setBranchType(BranchType.get(in.get()));

        if (in.remaining() < 2) {
            return ;
        }
        int resourceIdLen = in.getShort();
        if (resourceIdLen <= 0 || in.remaining() < resourceIdLen) {
            return ;
        }
        byte[] bs = new byte[resourceIdLen];
        in.get(bs);
        undoLogDeleteRequest.setResourceId(new String(bs, UTF8));

        if (in.remaining() < 2) {
            return ;
        }
        undoLogDeleteRequest.setSaveDays(in.getShort());
    }


}
