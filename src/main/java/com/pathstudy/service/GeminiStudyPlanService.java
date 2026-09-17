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
    public String generatePlan(String subjectName, int score, String level, List<String> weakTopics,
                               String referenceMaterial) {
        if (!isEnabled()) {
            return null;
        }
        String material = referenceMaterial == null ? "" : referenceMaterial;
        if (material.length() > 24000) {
            material = material.substring(0, 24000);
        }
        String prompt = """
                Bạn là gia sư %s cho học sinh THPT Việt Nam.
                Dưới đây là TÀI LIỆU HỌC (nguồn kiến thức chính thức, hãy bám sát nó):
                ====== TÀI LIỆU ======
                %s
                ====== HẾT TÀI LIỆU ======
                Học sinh vừa làm bài kiểm tra được %d/100 điểm (trình độ: %s).
                Các chủ đề làm sai (điểm yếu): %s.
                Dựa vào BÀI LÀM và TÀI LIỆU trên, hãy soạn bằng tiếng Việt, ngắn gọn, gồm:
                1) TỔNG HỢP ĐIỂM YẾU: nêu rõ học sinh yếu ở đâu (cụ thể, VD sai thì nào, cấu trúc nào).
                2) GIÁO TRÌNH ÔN TẬP: tóm tắt lý thuyết cốt lõi cho các điểm yếu, TRÍCH TỪ TÀI LIỆU (công thức, cách dùng, ví dụ).
                3) BÀI TẬP LUYỆN TẬP: 5 câu hỏi bám sát điểm yếu, KÈM ĐÁP ÁN ở cuối.
                Nếu là Tiếng Anh, thêm 10 từ vựng nên học theo chủ điểm.
                Chỉ trả về nội dung, không mở đầu dài dòng.
                """.formatted(subjectName, material, score, level,
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
