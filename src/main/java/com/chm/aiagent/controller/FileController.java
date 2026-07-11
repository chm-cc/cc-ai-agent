package com.chm.aiagent.controller;

import com.chm.aiagent.constant.FileConstant;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 文件下载控制器
 * <p>
 * 将服务器本地 download 目录下的文件通过 HTTP 提供给前端下载。
 * 支持图片预览（inline）和普通文件下载（attachment）。
 */
@Slf4j
@RestController
@RequestMapping("/api/files")
public class FileController {

    private static final String DOWNLOAD_DIR = FileConstant.FILE_SAVE_DIR + "/download";

    @GetMapping("/{filename}")
    public void serveFile(@PathVariable String filename,
                          HttpServletResponse response) throws IOException {
        File file = new File(DOWNLOAD_DIR, filename);

        if (!file.exists() || !file.isFile()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.setContentType(MediaType.TEXT_PLAIN_VALUE);
            response.getWriter().write("文件不存在: " + filename);
            return;
        }

        // 根据扩展名设置 Content-Type
        String contentType = probeContentType(filename);
        response.setContentType(contentType);
        response.setContentLengthLong(file.length());

        // 图片 → inline 预览；其他 → attachment 下载
        if (contentType.startsWith("image/")) {
            response.setHeader("Content-Disposition", "inline; filename=\"" +
                    URLEncoder.encode(filename, StandardCharsets.UTF_8) + "\"");
        } else {
            response.setHeader("Content-Disposition", "attachment; filename=\"" +
                    URLEncoder.encode(filename, StandardCharsets.UTF_8) + "\"");
        }

        // 流式写出文件
        try (FileInputStream fis = new FileInputStream(file);
             OutputStream os = response.getOutputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            os.flush();
        }

        log.info("文件已发送: {} ({} bytes, {})", filename, file.length(), contentType);
    }

    /** 根据文件扩展名探测 MIME 类型 */
    private String probeContentType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        if (lower.endsWith(".bmp")) return "image/bmp";
        if (lower.endsWith(".ico")) return "image/x-icon";
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".json")) return "application/json";
        if (lower.endsWith(".html") || lower.endsWith(".htm")) return "text/html";
        if (lower.endsWith(".css")) return "text/css";
        if (lower.endsWith(".js")) return "application/javascript";
        if (lower.endsWith(".mp4")) return "video/mp4";
        if (lower.endsWith(".mp3")) return "audio/mpeg";
        if (lower.endsWith(".zip")) return "application/zip";
        if (lower.endsWith(".txt")) return "text/plain";
        return "application/octet-stream";
    }
}
