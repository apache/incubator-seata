package org.apache.seata.integration.tx.api.json;

import org.apache.seata.common.exception.JsonParseException;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.integration.tx.api.interceptor.TwoPhaseBusinessActionParam;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JsonParserWrapTest {

    private JsonParserWrap parserWrap;
    private final String jsonString = "{\"actionName\":\"business_action\",\"useCommonFence\":null,\"businessActionContext\":null," +
            "\"branchType\":\"TCC\",\"delayReport\":null}";


    @BeforeEach
    public void setUp() {
        parserWrap = new JsonParserWrap(new JsonParserImpl());
    }

    @Test
    public void testToJSONString() {
        TwoPhaseBusinessActionParam actionParam = new TwoPhaseBusinessActionParam();
        actionParam.setActionName("business_action");
        actionParam.setBranchType(BranchType.TCC);

        String resultString = parserWrap.toJSONString(actionParam);

        assertEquals(jsonString, resultString);
    }

    @Test
    public void testToJSONStringThrowsException() {
        Object mockItem = mock(Object.class);
        when(mockItem.toString()).thenReturn(mockItem.getClass().getName());
        assertThrows(JsonParseException.class, () -> parserWrap.toJSONString(mockItem));
    }

    @Test
    public void testParseObject() {
        TwoPhaseBusinessActionParam actionParam = parserWrap.parseObject(jsonString, TwoPhaseBusinessActionParam.class);

        assertEquals("business_action", actionParam.getActionName());
        assertEquals(BranchType.TCC, actionParam.getBranchType());
    }

    @Test
    public void testParseObjectThrowsException() {
        assertThrows(JsonParseException.class, () -> parserWrap.parseObject(jsonString, Integer.class));
    }

    @Test
    public void testGetName() {
        assertEquals("customParser", parserWrap.getName());
    }
}
