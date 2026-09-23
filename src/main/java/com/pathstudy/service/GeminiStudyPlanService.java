package com.pathstudy.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

/**
 * Google Gemini implementation. Enabled only when app.ai.gemini-key is set
 * (via the GEMINI_API_KEY env var). On any error it returns null so the caller
 * falls back to a rule-based plan.
 */
@Service
public class GeminiStudyPlanService implements AiStudyPlanService {

    private static final Logger log = LoggerFactory.getLogger(GeminiStudyPlanService.class);
    private static final String ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/{m}:generateContent?key={k}";

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
                    .uri(ENDPOINT, model, apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            if (resp == null) {
                return null;
            }
            JsonNode text = resp.path("candidates").path(0).path("content").path("parts").path(0).path("text");
            return text.isMissingNode() ? null : text.asText();
        } catch (RestClientResponseException e) {
            log.warn("Gemini API error (model={}): HTTP {} - {}", model, e.getStatusCode().value(),
                    e.getResponseBodyAsString());
            return null;
        } catch (RuntimeException e) {
            log.warn("Gemini call failed (model={}): {}", model, e.toString());
            return null;
        }
    }

    /**
     * Admin diagnostic: reports whether the key/model are configured and makes a
     * minimal live call, returning the outcome (or the exact API error) as text.
     * Never reveals the full key — only its first characters and length.
     */
    @Override
    public String diagnose() {
        StringBuilder sb = new StringBuilder();
        sb.append("AI bật (isEnabled): ").append(isEnabled()).append('\n');
        sb.append("Model: ").append(model).append('\n');
        if (apiKey == null || apiKey.isBlank()) {
            sb.append("Key: CHƯA đặt (biến môi trường GEMINI_API_KEY trống).\n");
            return sb.toString();
        }
        String prefix = apiKey.length() >= 4 ? apiKey.substring(0, 4) : apiKey;
        sb.append("Key: bắt đầu '").append(prefix).append("...', độ dài ").append(apiKey.length());
        if (!apiKey.startsWith("AIza")) {
            sb.append("  ⚠ Key Gemini hợp lệ thường bắt đầu bằng 'AIza'.");
        }
        sb.append('\n');

        Map<String, Object> body = Map.of("contents",
                List.of(Map.of("parts", List.of(Map.of("text", "Trả lời đúng 2 chữ: xin chào")))));
        try {
            JsonNode resp = rest.post()
                    .uri(ENDPOINT, model, apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            JsonNode text = resp == null ? null
                    : resp.path("candidates").path(0).path("content").path("parts").path(0).path("text");
            sb.append("Gọi API: OK ✅\n");
            sb.append("Phản hồi mẫu: ").append(text == null ? "(rỗng)" : text.asText("(rỗng)"));
        } catch (RestClientResponseException e) {
            sb.append("Gọi API: LỖI ❌ HTTP ").append(e.getStatusCode().value()).append('\n');
            sb.append(e.getResponseBodyAsString());
        } catch (RuntimeException e) {
            sb.append("Gọi API: LỖI ❌ ").append(e.toString());
        }
        return sb.toString();
    }
}
