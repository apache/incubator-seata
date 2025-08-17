package org.apache.seata.mcp.controller.prompts;

import org.apache.seata.mcp.annotation.Prompt;
import org.apache.seata.mcp.annotation.PromptParam;
import org.springframework.stereotype.Service;

@Service
public class TestPrompt {

    @Prompt(description = "Analyze the code with the specified name")
    public String analyzeCode(@PromptParam(description = "code name") String name){
        return "Analyze this code:" + name;
    }
}
