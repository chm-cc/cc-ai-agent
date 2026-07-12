package com.chm.aiagent.agent;

import com.chm.aiagent.advisor.MyLoggerAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class CcManus extends ToolCallAgent {

    private static final String DEFAULT_SYSTEM_PROMPT = """
            You are CcManus, an all-capable AI assistant, aimed at solving any task presented by the user.
            You have various tools at your disposal that you can call upon to efficiently complete complex requests.
            """;

    private static final String NEXT_STEP_PROMPT = """
            Based on user needs, proactively select the most appropriate tool or combination of tools.
            For complex tasks, you can break down the problem and use different tools step by step to solve it.
            After using each tool, clearly explain the execution results and suggest the next steps.
            If you want to stop the interaction at any point, use the `terminate` tool/function call.
            """;

    public CcManus(ToolCallback[] allTools,
                   @Qualifier("dashscopeChatModel") ChatModel chatModel) {
        super(allTools);
        this.setName("ccManus");
        this.setSystemPrompt(DEFAULT_SYSTEM_PROMPT);
        this.setNextStepPrompt(NEXT_STEP_PROMPT);
        this.setMaxSteps(20);

        ChatClient chatClient = ChatClient.builder(chatModel)
                .defaultAdvisors(new MyLoggerAdvisor())
                .build();
        this.setChatClient(chatClient);
    }

    /** 配置 Agent 特定的 System Prompt（从 agents 表读取） */
    public void configure(String systemPrompt) {
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            this.setSystemPrompt(systemPrompt);
        }
    }
}
