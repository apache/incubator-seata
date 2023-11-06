package io.seata.saga.util;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;

public class ResourceUtilTests {

    @Test
    public void getResources_test() {
        Resource[] resources = ResourceUtil.getResources("classpath*:statelang/*.json");
        assertThat(resources.length).isEqualTo(6);

        Resource[] resources2 = ResourceUtil.getResources(new String[]{"classpath*:statelang/*.json"});
        assertThat(resources2.length).isEqualTo(6);
    }
}