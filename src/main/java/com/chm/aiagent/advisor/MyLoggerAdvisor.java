package com.chm.aiagent.advisor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.advisor.api.*;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * 自定义日志切面类
 * @author chm
 */

@Slf4j
@Component
public class MyLoggerAdvisor implements CallAroundAdvisor, StreamAroundAdvisor {
    @Override
    public AdvisedResponse aroundCall(AdvisedRequest advisedRequest, CallAroundAdvisorChain chain) {
        // 调用AI 前，可以进行一些处理
        advisedRequest = this.before(advisedRequest);
        AdvisedResponse advisedResponse = chain.nextAroundCall(advisedRequest);
        //  调用AI后，可以进行一些处理
        this.observeAfter(advisedResponse);
        return advisedResponse;
    }

    private AdvisedRequest before(AdvisedRequest request) {
        // 输出AI请求日志
        log.info("AI Request: {}", request.userText());
        return request;
    }

    private void observeAfter(AdvisedResponse advisedResponse) {
        // AI响应日志
        log.info("AI Response: {}", advisedResponse.response().getResult().getOutput().getText());
    }

    @Override
    public Flux<AdvisedResponse> aroundStream(AdvisedRequest advisedRequest, StreamAroundAdvisorChain chain) {
        return chain.nextAroundStream(advisedRequest)
                .doOnNext(advisedResponse -> {
                    String text = advisedResponse.response().getResult().getOutput().getText();
                    log.info("AI Stream Response: {}", text);
                });
    }


    @Override
    public String getName() {
        return this.getClass().getName();
    }

    @Override
    public int getOrder() {
        return 0;
    }


}
