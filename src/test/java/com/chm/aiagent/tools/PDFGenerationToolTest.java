package com.chm.aiagent.tools;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class PDFGenerationToolTest {

    @Test
    public void testGeneratePDF() {
        PDFGenerationTool tool = new PDFGenerationTool();
        String fileName = "百度.pdf";
        String content = "百度 https://www.baidu.com";
        String result = tool.generatePDF(fileName, content);
        assertNotNull(result);
    }
}