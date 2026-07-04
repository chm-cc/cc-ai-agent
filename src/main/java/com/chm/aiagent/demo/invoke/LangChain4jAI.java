package com.chm.aiagent.demo.invoke;

import dev.langchain4j.community.model.dashscope.QwenChatModel;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

//@Component
public class LangChain4jAI implements CommandLineRunner {

    @Override
    public void run(String... args) throws Exception {
        QwenChatModel build = QwenChatModel.builder()
                .apiKey("sk-ws-H.RYYMPHL.V84Y.MEUCIA2E-zs3ILHa6aUSMXNjnYTby4c8QkLevga12oWYEWKTAiEAkRdRHKD-pffS9N_xBtMWVDSK0tbpHomxgDiPGpX_ySs")
                .modelName("qwen-plus")
                .build();
        System.out.println(build.chat(
                String.valueOf(new Prompt(new AssistantMessage("Hello, 你是谁?")))
        ));
    }
}
