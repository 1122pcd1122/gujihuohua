package com.example.gujihuohua.service;

import com.example.gujihuohua.data.SentenceResult;
import com.example.gujihuohua.data.TextProcessRequest;
import com.github.houbb.opencc4j.util.ZhConverterUtil;
import org.mozilla.universalchardet.UniversalDetector;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class TextProcessService {

    public List<SentenceResult> processText(TextProcessRequest request, byte[] fileBytes) {
        String rawContent;

        // 1. 智能编码识别
        if (fileBytes != null && fileBytes.length > 0) {
            String detectedCharset = detectCharset(fileBytes);
            Charset charset = detectedCharset != null ? Charset.forName(detectedCharset) : StandardCharsets.UTF_8;
            rawContent = new String(fileBytes, charset);
        } else {
            rawContent = request.getContent();
        }

        if (rawContent == null) rawContent = "";

        // 2. 繁简转换 (Lombok 生成的方法是 isConvertToSimplified)
        if (request.isConvertToSimplified()) {
            rawContent = ZhConverterUtil.toSimple(rawContent);
        }

        // 3. 基础清洗
        String processingText = rawContent;
        if (request.isStandardizeSpaces()) {
            processingText = processingText.replaceAll("\\s+", " ").trim();
        }
        if (request.isRemoveBrackets()) {
            processingText = processingText.replaceAll("（.*?）", "").replaceAll("\\(.*?\\)", "");
        }
        if (request.isClearModernPunctuation()) {
            processingText = processingText.replaceAll("[《》“”‘’]", "");
        }

        // 4. 分句
        List<String> rawSentences = splitSentences(request, processingText);

        // 5. 封装结果
        List<SentenceResult> results = new ArrayList<>();
        int limit = request.isPreview() && rawSentences.size() > 20 ? 20 : rawSentences.size();

        for (int i = 0; i < limit; i++) {
            results.add(new SentenceResult(i + 1, rawSentences.get(i)));
        }

        return results;
    }

    private List<String> splitSentences(TextProcessRequest request, String text) {
        StringBuilder splitPattern = new StringBuilder();
        if (request.isSplitByNewline()) splitPattern.append("\n|");
        if (request.isSplitByPeriod()) splitPattern.append("。|\\.|");
        if (request.isSplitByComma()) splitPattern.append("，|,|、|");
        if (StringUtils.hasText(request.getCustomSeparator())) {
            splitPattern.append(Pattern.quote(request.getCustomSeparator())).append("|");
        }

        String regex = splitPattern.toString();
        if (regex.isEmpty()) {
            List<String> list = new ArrayList<>();
            list.add(text);
            return list;
        } else {
            if (regex.endsWith("|")) regex = regex.substring(0, regex.length() - 1);
            return Arrays.stream(text.split(regex))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        }
    }

    private String detectCharset(byte[] bytes) {
        UniversalDetector detector = new UniversalDetector(null);
        detector.handleData(bytes, 0, bytes.length);
        detector.dataEnd();
        String encoding = detector.getDetectedCharset();
        detector.reset();
        return encoding;
    }

    public String autoDetectType(String content) {
        if (content == null) return "未识别";
        if (content.contains("纪") || content.contains("传")) return "史书";
        if (content.contains("诗") || content.contains("曰")) return "笔记";
        return "未识别";
    }
}