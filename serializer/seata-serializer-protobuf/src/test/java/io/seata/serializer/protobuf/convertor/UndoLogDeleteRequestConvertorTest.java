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
package io.seata.serializer.protobuf.convertor;

import io.seata.serializer.protobuf.generated.UndoLogDeleteRequestProto;
import io.seata.core.model.BranchType;
import io.seata.core.protocol.transaction.UndoLogDeleteRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author yuanguoyao
 */
public class UndoLogDeleteRequestConvertorTest {

    private static final String RESOURCE_ID = "resourceId";
    private static final short SAVE_DAYS = 3;


    @Test
    public void convert2Proto() {

        UndoLogDeleteRequest undoLogDeleteRequest = new UndoLogDeleteRequest();
        undoLogDeleteRequest.setBranchType(BranchType.AT);
        undoLogDeleteRequest.setResourceId(RESOURCE_ID);
        undoLogDeleteRequest.setSaveDays(SAVE_DAYS);

        UndoLogDeleteRequestConvertor undoLogDeleteRequestConvertor = new UndoLogDeleteRequestConvertor();
        UndoLogDeleteRequestProto proto = undoLogDeleteRequestConvertor.convert2Proto(
                undoLogDeleteRequest);
        UndoLogDeleteRequest realRequest = undoLogDeleteRequestConvertor.convert2Model(proto);

        assertThat(realRequest.getTypeCode()).isEqualTo(undoLogDeleteRequest.getTypeCode());
        assertThat(realRequest.getBranchType()).isEqualTo(undoLogDeleteRequest.getBranchType());
        assertThat(realRequest.getResourceId()).isEqualTo(undoLogDeleteRequest.getResourceId());
        assertThat(realRequest.getSaveDays()).isEqualTo(undoLogDeleteRequest.getSaveDays());

    }
}