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
    // Gửi API key qua header x-goog-api-key (cách Google khuyến nghị; hỗ trợ cả
    // key kiểu cũ 'AIza...' lẫn kiểu mới 'AQ...'). Truyền qua ?key= dễ bị hiểu
    // nhầm là OAuth access token với key định dạng mới.
    private static final String ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/{m}:generateContent";
    private static final String LIST_ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models?pageSize=100";

    @Value("${app.ai.gemini-key:}")
    private String apiKey;

    @Value("${app.ai.gemini-model:gemini-1.5-flash}")
    private String model;

    private final RestClient rest = RestClient.create();

    /** Reason the last generatePlan call produced no AI text (shown in /admin/ai-check). */
    private volatile String lastError = "(chưa gọi generatePlan)";

    @Override
    public boolean isEnabled() {
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * Pulls the answer text out of a generateContent response, tolerating models
     * that return several parts (e.g. a thinking part before the answer): joins
     * every parts[*].text under the first candidate.
     */
    private static String extractText(JsonNode resp) {
        if (resp == null) {
            return null;
        }
        JsonNode parts = resp.path("candidates").path(0).path("content").path("parts");
        StringBuilder sb = new StringBuilder();
        for (JsonNode p : parts) {
            JsonNode t = p.path("text");
            if (t.isTextual()) {
                sb.append(t.asText());
            }
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    /**
     * POSTs a single-prompt generateContent request, retrying on transient
     * overload (HTTP 503 UNAVAILABLE / 429 rate limit) up to 3 attempts.
     */
    private JsonNode generateContent(String promptText) {
        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", promptText)))));
        RestClientResponseException lastEx = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                return rest.post()
                        .uri(ENDPOINT, model)
                        .header("x-goog-api-key", apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body)
                        .retrieve()
                        .body(JsonNode.class);
            } catch (RestClientResponseException e) {
                int code = e.getStatusCode().value();
                if ((code == 503 || code == 429) && attempt < 3) {
                    lastEx = e;
                    try {
                        Thread.sleep(700L * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw e;
                    }
                    continue;
                }
                throw e;
            }
        }
        throw lastEx;
    }

    @Override
    public String generatePlan(String subjectName, int score, String level, List<String> weakTopics,
                               String referenceMaterial) {
        if (!isEnabled()) {
            return null;
        }
        String material = referenceMaterial == null ? "" : referenceMaterial;
        // gemini-3.x có context rất lớn; giữ đủ tài liệu nhiều lớp (10/12) trong prompt.
        if (material.length() > 80000) {
            material = material.substring(0, 80000);
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
                ĐỊNH DẠNG: viết văn bản thuần tiếng Việt, dễ đọc. TUYỆT ĐỐI KHÔNG dùng
                ký hiệu Markdown (không dùng #, ##, ###, dấu * hay ** để in đậm) và
                KHÔNG dùng công thức LaTeX (không dùng $...$, \\text, _{}). Trình bày
                tiêu đề mục bằng chữ IN HOA, gạch đầu dòng bằng "-", ví dụ viết bình thường.
                """.formatted(subjectName, material, score, level,
                weakTopics.isEmpty() ? "chưa xác định" : String.join(", ", weakTopics));

        try {
            JsonNode resp = generateContent(prompt);
            String text = extractText(resp);
            if (text == null) {
                String dump = resp == null ? "null" : resp.toString();
                lastError = "200 nhưng không có text. finishReason/response: "
                        + dump.substring(0, Math.min(400, dump.length()));
                log.warn("Gemini returned no text (model={}): {}", model, lastError);
                return null;
            }
            lastError = "OK";
            return text;
        } catch (RestClientResponseException e) {
            lastError = "HTTP " + e.getStatusCode().value() + " - "
                    + e.getResponseBodyAsString();
            log.warn("Gemini API error (model={}): {}", model, lastError);
            return null;
        } catch (RuntimeException e) {
            lastError = e.toString();
            log.warn("Gemini call failed (model={}): {}", model, lastError);
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

        // Liệt kê các model khả dụng cho key này (để chọn đúng tên model).
        try {
            JsonNode list = rest.get()
                    .uri(LIST_ENDPOINT)
                    .header("x-goog-api-key", apiKey)
                    .retrieve()
                    .body(JsonNode.class);
            StringBuilder names = new StringBuilder();
            if (list != null && list.has("models")) {
                for (JsonNode m : list.path("models")) {
                    boolean gen = false;
                    for (JsonNode meth : m.path("supportedGenerationMethods")) {
                        if ("generateContent".equals(meth.asText())) { gen = true; break; }
                    }
                    if (gen) {
                        String n = m.path("name").asText().replace("models/", "");
                        names.append(names.length() == 0 ? "" : ", ").append(n);
                    }
                }
            }
            sb.append("Model khả dụng (generateContent): ")
                    .append(names.length() == 0 ? "(không đọc được)" : names).append('\n');
        } catch (RuntimeException e) {
            sb.append("Không liệt kê được model: ").append(e.toString()).append('\n');
        }

        try {
            JsonNode resp = generateContent("Trả lời đúng 2 chữ: xin chào");
            String text = extractText(resp);
            sb.append("Gọi API: OK ✅\n");
            sb.append("Phản hồi mẫu: ").append(text == null ? "(rỗng)" : text);
        } catch (RestClientResponseException e) {
            sb.append("Gọi API: LỖI ❌ HTTP ").append(e.getStatusCode().value()).append('\n');
            sb.append(e.getResponseBodyAsString());
        } catch (RuntimeException e) {
            sb.append("Gọi API: LỖI ❌ ").append(e.toString());
        }
        sb.append("\nLỗi gần nhất của generatePlan (bài kiểm tra thật): ").append(lastError);
        return sb.toString();
    }
}
