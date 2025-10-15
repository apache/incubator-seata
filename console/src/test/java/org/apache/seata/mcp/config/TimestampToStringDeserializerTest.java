package org.apache.seata.mcp.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TimestampToStringDeserializerTest {

    @Test
    void testDeserializeFormatsTimestamp() throws IOException {
        TimestampToStringDeserializer deserializer = new TimestampToStringDeserializer();
        JsonParser parser = mock(JsonParser.class);
        DeserializationContext context = mock(DeserializationContext.class);

        long millis = 1577934245000L;
        when(parser.getLongValue()).thenReturn(millis);

        String result = deserializer.deserialize(parser, context);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String expected = LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault())
                .format(formatter);

        assertEquals(expected, result);
    }
}
