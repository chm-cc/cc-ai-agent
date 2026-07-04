package com.chm.aiagent.controller;

import com.chm.aiagent.app.CarApp;
import jakarta.annotation.Resource;
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


    @GetMapping("/sse")
    public SseEmitter sse(@RequestParam String message,
                          @RequestParam String chatId){
        return carApp.doChatStream(message, chatId);
    }
    //@GetMapping("/manus/chat")
    //    public SseEmitter doChatWithManus(String message) {
    //        YuManus yuManus = new YuManus(allTools, dashscopeChatModel);
    //        return yuManus.runStream(message);
    //    }
}
