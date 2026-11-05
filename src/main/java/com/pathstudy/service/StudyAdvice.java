package com.pathstudy.service;

/**
 * Lời khuyên luyện tập ĐẶC THÙ THEO MÔN cho kế hoạch học rule-based (khi chưa
 * bật AI).
 *
 * <p>Trước đây kế hoạch dự phòng viết cứng câu "bổ sung từ vựng" cho mọi môn —
 * vốn chỉ đúng với Tiếng Anh — nên học sinh làm bài Toán vẫn bị khuyên đi học
 * từ vựng. Mọi câu khuyên gắn với một môn cụ thể phải đi qua đây.
 */
public final class StudyAdvice {

    private StudyAdvice() {
    }

    /** Việc nên làm thêm, hợp với đặc thù môn. Không kèm dấu "•". */
    public static String practiceLine(String subjectCode) {
        if (subjectCode == null) {
            return "Luyện thêm bài tập theo từng dạng để nắm chắc kiến thức.";
        }
        switch (subjectCode) {
            case "anh":
                return "Bổ sung 20–30 từ vựng mỗi tuần theo chủ điểm và luyện nghe 15 phút/ngày.";
            case "van":
                return "Đọc thêm 1–2 văn bản cùng thể loại và luyện viết đoạn nghị luận ngắn.";
            case "toan":
                return "Học kỹ công thức trọng tâm và trình bày lại lời giải theo từng bước.";
            case "ly":
                return "Nắm chắc công thức và đơn vị đo, luyện bài tập tính toán theo từng dạng.";
            case "hoa":
                return "Ôn lại phương trình phản ứng và luyện bài tập tính theo phương trình.";
            case "sinh":
                return "Hệ thống kiến thức bằng sơ đồ tư duy và luyện câu hỏi vận dụng.";
            default:
                return "Luyện thêm bài tập theo từng dạng để nắm chắc kiến thức.";
        }
    }

    /**
     * Mục tiêu ngắn gọn in ở đầu trang lộ trình. Trước đây câu này viết cứng
     * "nâng cao kỹ năng phân tích và nghị luận văn học" cho MỌI môn, nên học
     * sinh mở lộ trình Vật lý vẫn thấy mục tiêu môn Văn.
     */
    public static String goalLine(String subjectCode) {
        if (subjectCode == null) {
            return "nắm chắc kiến thức nền và luyện đề đều đặn";
        }
        switch (subjectCode) {
            case "van":
                return "nâng cao kỹ năng phân tích và nghị luận văn học";
            case "anh":
                return "mở rộng vốn từ, chắc ngữ pháp và luyện kỹ năng đọc";
            case "toan":
                return "nắm chắc công thức và trình bày lời giải chặt chẽ";
            case "ly":
                return "hiểu bản chất hiện tượng và thạo bài tập tính toán";
            case "hoa":
                return "nắm chắc phương trình phản ứng và bài tập tính theo phương trình";
            case "sinh":
                return "hệ thống kiến thức và làm tốt câu hỏi vận dụng";
            default:
                return "nắm chắc kiến thức nền và luyện đề đều đặn";
        }
    }

    /** Gợi ý khi học sinh không có chủ đề nào yếu. Không kèm dấu "•". */
    public static String strongLine(String subjectCode) {
        if ("anh".equals(subjectCode)) {
            return "Hãy luyện đề tổng hợp để nâng điểm và bổ sung 20–30 từ vựng mỗi tuần.";
        }
        if ("van".equals(subjectCode)) {
            return "Hãy luyện đề tổng hợp và viết thêm bài nghị luận để nâng điểm.";
        }
        return "Hãy luyện đề tổng hợp và thử các câu vận dụng cao để nâng điểm.";
    }
}
