package com.chm.aiagent.app;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

@SpringBootTest
class CarAppTest {

    @Resource
    private ChatModel dashscopeChatModel;

    @Resource
    private CarApp carApp;

    @Test
    void doChatSSE() {
        String chatId = UUID.randomUUID().toString();
        // 第一轮对话
        carApp.doChat("你好，我是剪子", chatId);
    }

    @Test
    void doChat() {
        String chatId = UUID.randomUUID().toString();
        // 第一轮对话
        carApp.doChat("你好，我是剪子", chatId);
        // 第二轮对话
        carApp.doChat("我想让另一半（胡图图）更爱我", chatId);
        // 第三轮对话
        carApp.doChat("我的另一半叫什么来着？刚跟你说过，帮我回忆一下,直接用一句话回答我", chatId);
    }

    @Test
    void doChatWithReport() {
        String chatId = UUID.randomUUID().toString();
        String message = "你好，我是大美女剪子，我想让另一半（胡图图）更爱我，但我不知道该怎么做";
        CarApp.LoveReport loveReport = carApp.doChatWithReport(message, chatId);
        Assertions.assertNotNull(loveReport);
    }

    @Test
    void testDoChatWithRag() {
        String chatId = UUID.randomUUID().toString();
        String message = "我要买新车，那旧车是置换给 4S 店划算还是自己卖划算";
        String content = carApp.doChatWithRag(message, chatId);
        Assertions.assertNotNull(content);
    }
    @Test
    void doChatWithTools() {

        testMessage("周末想带朋友去北京去看4s店看车，推荐几个适合的地方？");

        testMessage("最近想买车了，看看百度上（www.baidu.com）有推荐购买的车型吗？");

        testMessage("直接下载一张适合做手机壁纸的星空图片为文件");

        testMessage("执行 Python3 脚本来生成数据分析报告");

        testMessage("生成一份‘百度111’PDF，包含百度的访问地址");

        testMessage("保存我的买车档案为文件");
    }

    private void testMessage(String message) {
        String chatId = UUID.randomUUID().toString();
        String answer = carApp.doChatWithTools(message, chatId);
        Assertions.assertNotNull(answer);
    }

//    @Test
//    void doChatWithMcp() {
//        String chatId = UUID.randomUUID().toString();
//        String message = "我的另一半居住在上海静安区，请帮我找到 5 公里内合适的约会地点";
//        String answer =  loveApp.doChatWithMcp(message, chatId);
//    }


    @Test
    void doChatWithMcp() {
        String chatId = UUID.randomUUID().toString();
        String message = "帮我搜索随便两张车的图片";
        String answer =  carApp.doChatWithMcp(message, chatId);
        Assertions.assertNotNull(answer);
    }

}