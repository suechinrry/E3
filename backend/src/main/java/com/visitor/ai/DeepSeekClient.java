package com.visitor.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class DeepSeekClient {

    private final String apiKey;
    private final String apiUrl;
    private final WebClient webClient;

    public DeepSeekClient(@Value("${deepseek.api-key}") String apiKey,
                          @Value("${deepseek.api-url}") String apiUrl) {
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
        this.webClient = WebClient.create();
    }

    @SuppressWarnings("unchecked")
    public Map<String, String> generateGreeting(String visitorName, String company,
                                                 String purpose, String hostName) {
        if (apiKey == null || apiKey.isBlank()) {
            log.info("DeepSeek API Key 未配置，返回默认话术");
            return mockGreeting(visitorName, company, purpose, hostName);
        }

        String prompt = String.format(
                "你是一个企业前台接待助手。请根据以下访客信息生成一段迎接话术（包含欢迎语、座位安排建议、注意事项）。" +
                "请严格按照JSON格式返回，不要包含markdown代码块。\n\n" +
                "访客姓名：%s\n来访单位：%s\n访问事由：%s\n被访人：%s\n\n" +
                "返回格式：{\"greeting\":\"...\",\"seatSuggestion\":\"...\",\"notes\":\"...\"}",
                visitorName, company, purpose, hostName);

        try {
            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("model", "deepseek-chat");
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
                    return parseResult(content);
                }
            }
        } catch (Exception e) {
            log.error("调用DeepSeek API失败", e);
        }
        return mockGreeting(visitorName, company, purpose, hostName);
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> parseResult(String content) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(content, Map.class);
        } catch (Exception e) {
            log.warn("解析AI返回JSON失败，使用默认话术");
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
