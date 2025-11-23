package com.example.gujihuohua.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 结构化句子结果
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SentenceResult {
    private int index;          // 句序
    private String content;     // 内容
    private int textLength;     // 长度 (可选)

    // 为了方便使用，保留一个双参数构造器（或者直接用 @AllArgsConstructor 传三个参数）
    public SentenceResult(int index, String content) {
        this.index = index;
        this.content = content;
        this.textLength = content.length();
    }
}