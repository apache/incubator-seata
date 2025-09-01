package org.apache.seata.mcp.controller.resources;

import io.modelcontextprotocol.spec.McpSchema;
import org.apache.seata.mcp.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class TestResource {

    @Resource(description = "test resource", uri = "file://test/testText", mimeType = "text")
    public McpSchema.TextResourceContents testResource(String uri) {
        return new McpSchema.TextResourceContents("666", "text", uri);
    }
}
