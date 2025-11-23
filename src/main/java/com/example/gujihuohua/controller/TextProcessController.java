package com.example.gujihuohua.controller;

import com.example.gujihuohua.data.SentenceResult;
import com.example.gujihuohua.data.TextProcessRequest;
import com.example.gujihuohua.service.TextProcessService;
import lombok.RequiredArgsConstructor; // 使用 Lombok 自动注入构造函数
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/text")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor // Lombok 会自动生成包含 final 字段的构造函数
public class TextProcessController {

    private final TextProcessService textProcessService;

    @PostMapping("/process")
    public ResponseEntity<Map<String, Object>> processText(
            @RequestPart(value = "file", required = false) MultipartFile file,
            @RequestPart("config") TextProcessRequest config) {

        try {
            byte[] fileBytes = null;
            if (file != null && !file.isEmpty()) {
                fileBytes = file.getBytes();
            } else if (config.getContent() != null) {
                fileBytes = config.getContent().getBytes(StandardCharsets.UTF_8);
            }

            if (fileBytes == null || fileBytes.length == 0) {
                // 使用兼容 Java 8 的写法
                Map<String, Object> err = new HashMap<>();
                err.put("error", "无有效内容");
                return ResponseEntity.badRequest().body(err);
            }

            List<SentenceResult> results = textProcessService.processText(config, fileBytes);

            Map<String, Object> response = new HashMap<>();
            response.put("totalSentences", results.size());
            response.put("lines", results);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> err = new HashMap<>();
            err.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(err);
        }
    }

    @PostMapping("/detect-type")
    public ResponseEntity<String> detectType(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(textProcessService.autoDetectType(body.get("content")));
    }
}