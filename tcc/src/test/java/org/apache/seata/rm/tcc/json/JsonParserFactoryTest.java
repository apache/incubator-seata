package org.apache.seata.rm.tcc.json;

import org.apache.seata.common.loader.EnhancedServiceNotFoundException;
import org.apache.seata.integration.tx.api.json.JsonParserFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class JsonParserFactoryTest {

    @Test
    public void testGetInstance() {
        assertNotNull(JsonParserFactory.getInstance("jackson"));
    }

    @Test
    public void testGetInstanceThrowsException() {
        assertThrows(EnhancedServiceNotFoundException.class, () -> JsonParserFactory.getInstance("jsonParser"));
    }
}
