package com.visitor.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;

@Slf4j
@Service
public class DeepSeekClient {

    private final String apiKey;
    private final String apiUrl;
    private final String model;
    private final WebClient webClient;

    public DeepSeekClient(@Value("${deepseek.api-key}") String apiKey,
                          @Value("${deepseek.api-url}") String apiUrl,
                          @Value("${deepseek.model}") String model) {
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
        this.model = model;
        this.webClient = WebClient.create();
    }

    /** 返回 Map 中包含 source 字段："ai" 表示 AI 生成，"mock" 表示模板兜底 */
    @SuppressWarnings("unchecked")
    public Map<String, String> generateGreeting(String visitorName, String company,
                                                 String purpose, String hostName) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("[AI] API Key 未配置，使用模板话术");
            Map<String, String> result = mockGreeting(visitorName, company, purpose, hostName);
            result.put("source", "mock");
            return result;
        }

        String prompt = String.format(
                "你是一个企业前台接待助手。请根据以下访客信息生成一段迎接话术（包含欢迎语、座位安排建议、注意事项）。" +
                "请严格按照JSON格式返回，不要包含markdown代码块。\n\n" +
                "访客姓名：%s\n来访单位：%s\n访问事由：%s\n被访人：%s\n\n" +
                "返回格式：{\"greeting\":\"...\",\"seatSuggestion\":\"...\",\"notes\":\"...\"}",
                visitorName, company, purpose, hostName);

        try {
            log.info("[AI] 开始调用模型: model={}, visitor={}", model, visitorName);
            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", List.of(Map.of("role", "user", "content", prompt)));
            requestBody.put("temperature", 0.7);

            Map<String, Object> response = webClient.post()
                    .uri(apiUrl)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    String content = (String) message.get("content");
                    Map<String, String> result = parseResult(content);
                    result.put("source", "ai");
                    log.info("[AI] ✅ 调用成功，话术已生成");
                    return result;
                }
            }
            log.warn("[AI] 响应格式异常，回退模板");
        } catch (Exception e) {
            log.error("[AI] ❌ 调用失败: {}", e.getMessage());
        }
        Map<String, String> result = mockGreeting(visitorName, company, purpose, hostName);
        result.put("source", "mock");
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> parseResult(String content) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, String> map = mapper.readValue(content, Map.class);
            log.info("[AI] JSON解析成功");
            return map;
        } catch (Exception e) {
            log.warn("[AI] 解析返回JSON失败，使用模板话术");
            return mockGreeting("访客", "来访单位", "访问", "被访人");
        }
    }

    private Map<String, String> mockGreeting(String name, String company,
                                              String purpose, String hostName) {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("greeting", String.format(
                "欢迎%s的%s先生/女士莅临我司%s，%s已在会议室等候，请前台引导。",
                company, name, purpose, hostName));
        result.put("seatSuggestion", "建议安排在会议室A，已准备投影设备和茶水。");
        result.put("notes", "来访人员信息已通知被访人，请前台准备访客证。");
        return result;
    }
}
