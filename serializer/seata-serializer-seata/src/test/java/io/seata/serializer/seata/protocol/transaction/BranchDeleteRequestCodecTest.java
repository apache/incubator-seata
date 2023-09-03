package io.seata.serializer.seata.protocol.transaction;

import io.seata.core.model.BranchType;
import io.seata.core.protocol.transaction.BranchDeleteRequest;
import io.seata.serializer.seata.SeataSerializer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class BranchDeleteRequestCodecTest {

    /**
     * The Seata codec.
     */
    SeataSerializer seataSerializer = new SeataSerializer();

    /**
     * Test codec.
     */
    @Test
    public void test_codec(){
        BranchDeleteRequest branchDeleteRequest = new BranchDeleteRequest();
        branchDeleteRequest.setBranchId(112232);
        branchDeleteRequest.setBranchType(BranchType.TCC);
        branchDeleteRequest.setResourceId("343");
        branchDeleteRequest.setXid("123");

        byte[] bytes = seataSerializer.serialize(branchDeleteRequest);

        BranchDeleteRequest branchDeleteRequest2 = seataSerializer.deserialize(bytes);

        assertThat(branchDeleteRequest2.getBranchId()).isEqualTo(branchDeleteRequest.getBranchId());
        assertThat(branchDeleteRequest2.getBranchType()).isEqualTo(branchDeleteRequest.getBranchType());
        assertThat(branchDeleteRequest2.getResourceId()).isEqualTo(branchDeleteRequest.getResourceId());
        assertThat(branchDeleteRequest2.getXid()).isEqualTo(branchDeleteRequest.getXid());

    }
}
