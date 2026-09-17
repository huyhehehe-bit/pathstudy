package com.pathstudy.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Google Gemini implementation. Enabled only when app.ai.gemini-key is set
 * (via the GEMINI_API_KEY env var). On any error it returns null so the caller
 * falls back to a rule-based plan.
 */
@Service
public class GeminiStudyPlanService implements AiStudyPlanService {

    @Value("${app.ai.gemini-key:}")
    private String apiKey;

    @Value("${app.ai.gemini-model:gemini-1.5-flash}")
    private String model;

    private final RestClient rest = RestClient.create();

    @Override
    public boolean isEnabled() {
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    public String generatePlan(String subjectName, int score, String level, List<String> weakTopics) {
        if (!isEnabled()) {
            return null;
        }
        String prompt = """
                Bạn là gia sư %s cho học sinh THPT Việt Nam. Học sinh vừa làm bài kiểm tra
                được %d/100 điểm (trình độ: %s). Các chủ đề còn yếu: %s.
                Hãy soạn bằng tiếng Việt, ngắn gọn, gồm 3 phần rõ ràng:
                1) LỘ TRÌNH ÔN TẬP: 4-6 gạch đầu dòng, TẬP TRUNG vào đúng các chủ đề yếu ở trên.
                2) GIÁO TRÌNH NGẮN: tóm tắt lý thuyết cốt lõi của chủ đề yếu nhất (công thức/cách dùng, ví dụ).
                3) BÀI TẬP: 4-5 câu bài tập cho chủ đề yếu nhất, KÈM ĐÁP ÁN ở cuối.
                Nếu là Tiếng Anh, thêm 10 từ vựng nên học theo chủ điểm.
                Chỉ trả về nội dung, không mở đầu dài dòng.
                """.formatted(subjectName, score, level,
                weakTopics.isEmpty() ? "chưa xác định" : String.join(", ", weakTopics));

        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))));

        try {
            JsonNode resp = rest.post()
                    .uri("https://generativelanguage.googleapis.com/v1beta/models/{m}:generateContent?key={k}",
                            model, apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            if (resp == null) {
                return null;
            }
            JsonNode text = resp.path("candidates").path(0).path("content").path("parts").path(0).path("text");
            return text.isMissingNode() ? null : text.asText();
        } catch (RuntimeException e) {
            return null;
        }
    }
}
