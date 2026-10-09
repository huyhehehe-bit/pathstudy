package com.pathstudy.web;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Mục tiêu học sinh chọn khi vào trang chủ ("Bạn đang cần gì?") và khi đăng ký.
 *
 * <p>Lưu mã ngắn vào {@code User.goal} để sau này đo được người dùng mới vào
 * vì nhu cầu gì, và để gợi ý lộ trình phù hợp. Nhãn hiển thị tra ở đây để
 * landing + form đăng ký + trang quản trị dùng chung một nguồn.
 */
public final class StudyGoal {

    private StudyGoal() {
    }

    /** mã -> nhãn hiển thị. LinkedHashMap để giữ thứ tự hiển thị. */
    public static final Map<String, String> ALL = new LinkedHashMap<>();

    /** mã -> mô tả ngắn hiện trên thẻ ở landing. */
    public static final Map<String, String> BLURB = new LinkedHashMap<>();

    static {
        ALL.put("mat-goc", "Mình đang mất gốc");
        BLURB.put("mat-goc", "Xây lại nền từ kiến thức cơ bản nhất, đi chậm mà chắc.");

        ALL.put("len-diem", "Muốn cải thiện điểm trên lớp");
        BLURB.put("len-diem", "Vá đúng chỗ hổng để lên điểm kiểm tra và thi học kì.");

        ALL.put("thi-thpt", "Ôn thi tốt nghiệp THPT");
        BLURB.put("thi-thpt", "Luyện theo cấu trúc đề thi tốt nghiệp, bám sát trọng tâm.");

        ALL.put("kiem-tra", "Chỉ muốn kiểm tra trình độ");
        BLURB.put("kiem-tra", "Làm bài test đầu vào miễn phí để biết mình đang ở đâu.");
    }

    /** Hợp lệ hoá mã nhận từ URL/form; không hợp lệ thì trả null. */
    public static String normalize(String code) {
        if (code == null) {
            return null;
        }
        String c = code.strip();
        return ALL.containsKey(c) ? c : null;
    }

    public static String label(String code) {
        return ALL.get(code);
    }
}
