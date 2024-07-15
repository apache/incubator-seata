package org.apache.seata.server.controller;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;


@Disabled
@SpringBootTest
class NamingControllerTest {
    @Autowired
    private NamingController namingController;

    @Test
    void addVGroup() {
        namingController.addVGroup("group1","unit1");
    }

    @Test
    void removeVGroup() {
        namingController.removeVGroup("group1");
    }
}