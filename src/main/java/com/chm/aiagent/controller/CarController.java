package com.chm.aiagent.controller;

import com.chm.aiagent.agent.CcManus;
import com.chm.aiagent.app.CarApp;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/ai/car")
public class CarController {

    @Resource
    private CarApp carApp;

    @Resource
    private ObjectProvider<CcManus> ccManusProvider;

    @GetMapping("/sse")
    public SseEmitter sse(@RequestParam String message,
                          @RequestParam String chatId) {
        return carApp.doChatStream(message, chatId);
    }

    @GetMapping("/manus/chat")
    public SseEmitter doChatWithManus(String message) {
        // prototype 作用域，每次 getObject() 返回新实例
        CcManus ccManus = ccManusProvider.getObject();
        return ccManus.runStream(message);
    }
}
