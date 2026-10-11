package com.pathstudy.web.dto;

import java.io.Serializable;
import java.util.List;

/**
 * Dữ liệu cần để AI soạn giáo trình, cất tạm trong session sau khi chấm bài.
 *
 * <p>Vì sao đi qua session chứ không nhận từ client: nếu trình duyệt gửi lên
 * điểm/điểm yếu/tên môn thì người dùng sửa được payload và bơm nội dung tuỳ ý
 * vào prompt Gemini bằng API key của dự án. Client chỉ cầm một khoá ngẫu nhiên
 * vô nghĩa; mọi nội dung thật nằm ở server.
 */
public record StudyPlanContext(
        String subjectCode,
        int score,
        String level,
        List<String> weakTopics
) implements Serializable {
    // Cố ý KHÔNG mang theo tài liệu tham chiếu: nó có thể tới 200k ký tự, nhân
    // với mỗi học sinh đang mở trang kết quả là session phình rất nhanh.
    // AiController nạp tài liệu từ DB đúng lúc gọi Gemini.
}
