package org.apache.seata.tm.api.transaction;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class SuspendedResourcesHolderTest {

    @Test
    void getTest(){
        Assertions.assertThrows(IllegalArgumentException.class,()->{
            new SuspendedResourcesHolder(null);
        });
    }

    @Test
    void getXidTest(){
        SuspendedResourcesHolder suspendedResourcesHolder = new SuspendedResourcesHolder("123");
        Assertions.assertEquals(suspendedResourcesHolder.getXid(),"123");
    }
}
