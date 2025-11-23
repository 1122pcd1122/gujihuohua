package com.example.gujihuohua.data;

import lombok.Data;

@Data
public class TextProcessRequest {
    // 基础字段
    private String inputType;
    private String content;
    private String textCategory;

    // 功能开关
    private boolean convertToSimplified; // 新增：繁简转换

    // 分句规则
    private boolean splitByPeriod;
    private boolean splitByNewline;
    private boolean splitByComma;
    private String customSeparator;

    // 清洗规则
    private boolean removeBrackets;
    private boolean clearModernPunctuation;
    private boolean standardizeSpaces;

    // 预览控制
    private boolean isPreview;
}