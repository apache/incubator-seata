package org.apache.seata.mcp.register;

import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.apache.seata.mcp.manager.McpServerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

@Component
public class MCPAutoPromptRegister {
    @Autowired
    private Environment env;

    private final McpServerManager asyncServer;

    private static final Logger LOGGER = LoggerFactory.getLogger(MCPAutoPromptRegister.class);

    public MCPAutoPromptRegister(McpServerManager asyncServer) {
        this.asyncServer = asyncServer;
    }

    @PostConstruct
    public void init() {
        String systemDescription = env.getProperty("seata.mcp.prompts.systemPrompt.description");
        List<McpSchema.PromptMessage> messages = new ArrayList<>();
        messages.add(new McpSchema.PromptMessage(
                McpSchema.Role.ASSISTANT, new McpSchema.TextContent(null, 0.0, systemDescription)));
        McpServerFeatures.AsyncPromptSpecification systemPrompt = new McpServerFeatures.AsyncPromptSpecification(
                new McpSchema.Prompt(
                        null,
                        "The basic prompt, all questions about seata need to refer to this prompt",
                        "system-prompt"),
                (exchange, request) -> {
                    return Mono.just(new McpSchema.GetPromptResult("System prompt", messages));
                });
        asyncServer
                .getServerInstance()
                .addPrompt(systemPrompt)
                .doOnError(error -> {
                    LOGGER.error("Prompt registration failed:{}", error.getMessage());
                    throw new RuntimeException("Failed to register the system prompt: ", error);
                })
                .subscribe();
    }
}
