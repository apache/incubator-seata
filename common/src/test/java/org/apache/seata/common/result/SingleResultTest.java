package org.apache.seata.common.result;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SingleResultTest {

    @Test
    void testConstructor() {
        SingleResult<String> result = new SingleResult<>("200", "OK", "Data");
        Assertions.assertEquals("200", result.getCode());
        Assertions.assertEquals("OK", result.getMessage());
        Assertions.assertEquals("Data", result.getData());
    }

    @Test
    void testFailureWithCodeAndMessage() {
        SingleResult<String> result = SingleResult.failure("500", "Error");
        Assertions.assertEquals("500", result.getCode());
        Assertions.assertEquals("Error", result.getMessage());
        Assertions.assertNull(result.getData());
    }

    @Test
    void testFailureWithErrorCode() {
        SingleResult<String> result = SingleResult.failure(Code.LOGIN_FAILED);
        Assertions.assertEquals("401", result.getCode());
        Assertions.assertEquals("Login failed", result.getMessage());
        Assertions.assertNull(result.getData());
    }

    @Test
    void testSuccess() {
        SingleResult<String> result = SingleResult.success("SuccessData");
        Assertions.assertEquals(SingleResult.SUCCESS_CODE, result.getCode());
        Assertions.assertEquals(SingleResult.SUCCESS_MSG, result.getMessage());
        Assertions.assertEquals("SuccessData", result.getData());
    }

    @Test
    void testGettersAndSetters() {
        SingleResult<String> result = new SingleResult<>("200", "OK");
        result.setData("NewData");
        Assertions.assertEquals("NewData", result.getData());
    }
}
