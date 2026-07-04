package com.chm.aiagent.rag;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CarAppVectorStoreConfigTest {

    @Resource
    private LoveAppVectorStoreConfig loveAppVectorStoreConfig;
    @Test
    void loveAppVectorStore() {
        loveAppVectorStoreConfig.loveAppVectorStore(null);
    }
}