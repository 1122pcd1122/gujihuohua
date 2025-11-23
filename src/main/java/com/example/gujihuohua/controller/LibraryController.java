package com.example.gujihuohua.controller;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.gujihuohua.entity.AncientText;
import com.example.gujihuohua.service.LibraryService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/library")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class LibraryController {

    private final LibraryService libraryService;

    @Value("${app.storage.root-path}")
    private String storageRoot;

    // 1. 保存接口 (写文件 + 写库)
    @PostMapping("/save")
    public ResponseEntity<?> saveText(@RequestBody Map<String, Object> payload) {
        try {
            String customTitle = (String) payload.get("title"); // 用户输入的自定义标题
            String category = (String) payload.get("category");
            String rawContent = (String) payload.get("rawContent");
            List<?> jsonData = (List<?>) payload.get("data");

            // 1. 构建文件名: 标题_时间戳
            String timeStr = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String fileNameBase = customTitle + "_" + timeStr;

            // 2. 准备目录 (按日期归档: data-storage/2025-11-21/)
            String dateFolder = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
            Path dirPath = Paths.get(storageRoot, dateFolder);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }

            // 3. 保存 TXT
            String txtFileName = fileNameBase + ".txt";
            Path txtPath = dirPath.resolve(txtFileName);
            if (rawContent != null) {
                Files.write(txtPath, rawContent.getBytes(StandardCharsets.UTF_8));
            }

            // 4. 保存 JSON
            String jsonFileName = fileNameBase + ".json";
            Path jsonPath = dirPath.resolve(jsonFileName);
            String jsonString = JSON.toJSONString(jsonData);
            Files.write(jsonPath, jsonString.getBytes(StandardCharsets.UTF_8));

            // 5. 存入数据库 (只存相对路径，方便迁移)
            AncientText text = new AncientText();
            text.setTitle(customTitle);
            text.setCategory(category);
            text.setSentenceCount((Integer) payload.get("count"));
            text.setCreateTime(LocalDateTime.now());
            // 存相对路径：2025-11-21/xxx.txt
            text.setRawPath(dateFolder + "/" + txtFileName);
            text.setJsonPath(dateFolder + "/" + jsonFileName);

            libraryService.save(text);
            return ResponseEntity.ok("保存成功");

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("文件存储失败: " + e.getMessage());
        }
    }

    // 2. 获取列表 (不变，只查数据库)
    @GetMapping("/list")
    public ResponseEntity<List<AncientText>> getList() {
        List<AncientText> list = libraryService.list(new LambdaQueryWrapper<AncientText>()
                .select(AncientText::getId, AncientText::getTitle, AncientText::getCategory,
                        AncientText::getSentenceCount, AncientText::getCreateTime)
                .orderByDesc(AncientText::getCreateTime));
        return ResponseEntity.ok(list);
    }

    // 3. 获取详情 (读文件返回给前端)
    @GetMapping("/detail/{id}")
    public ResponseEntity<Map<String, Object>> getDetail(@PathVariable Long id) {
        AncientText text = libraryService.getById(id);
        if (text == null) return ResponseEntity.notFound().build();

        Map<String, Object> response = new HashMap<>();
        response.put("info", text); // 元数据

        // 读取 JSON 文件内容
        try {
            Path jsonFullPath = Paths.get(storageRoot, text.getJsonPath());
            if (Files.exists(jsonFullPath)) {
                String content = new String(Files.readAllBytes(jsonFullPath), StandardCharsets.UTF_8);
                response.put("processedJson", content); // 发给前端渲染
            } else {
                response.put("processedJson", "[]"); // 文件丢失的情况
            }
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }

        return ResponseEntity.ok(response);
    }

    // 4. 删除 (删库 + 删文件)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        AncientText text = libraryService.getById(id);
        if (text != null) {
            // 尝试删除物理文件
            try {
                Files.deleteIfExists(Paths.get(storageRoot, text.getJsonPath()));
                Files.deleteIfExists(Paths.get(storageRoot, text.getRawPath()));
            } catch (IOException e) {
                // 文件删除失败不影响删库
                e.printStackTrace();
            }
            libraryService.removeById(id);
        }
        return ResponseEntity.ok("已删除");
    }
}