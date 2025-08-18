package io.modelcontextprotocol.server;

import org.springframework.web.servlet.function.ServerRequest;

public class WebMvcContextExtractor implements McpTransportContextExtractor<ServerRequest> {

    @Override
    public McpTransportContext extract(ServerRequest request, McpTransportContext context) {
        return context;
    }
}
