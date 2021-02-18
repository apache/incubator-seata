package io.seata.plugin.jackson.parser.oracle;

import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.rm.datasource.undo.parser.spi.JacksonSerializer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * @author jsbxyyx
 */
public class OracleTimestampJacksonSerializerTest {

    @Test
    public void test_oracleJacksonSerializer() throws Exception {
        List<JacksonSerializer> serializers = EnhancedServiceLoader.loadAll(JacksonSerializer.class);
        Assertions.assertTrue(serializers.size() > 0, "Jackson Serializer is empty");
        OracleTimestampJacksonSerializer s = null;
        for (JacksonSerializer serializer : serializers) {
            if (serializer instanceof OracleTimestampJacksonSerializer) {
                s = (OracleTimestampJacksonSerializer) serializer;
                break;
            }
        }
        Assertions.assertNotNull(s);
    }

}
