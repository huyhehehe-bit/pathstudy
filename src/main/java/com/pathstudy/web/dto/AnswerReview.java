package com.pathstudy.web.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * Một câu trong phần "xem lại bài làm": học sinh chọn gì, đáp án đúng là gì.
 *
 * <p>Dùng cho cả đề luyện tập và bài kiểm tra đầu vào, để học sinh thấy SAI Ở
 * ĐÂU chứ không chỉ thấy điểm. Nút giải thích sẽ gọi Gemini theo
 * {@code questionId} + {@code chosenIndex}.
 */
@Getter
@AllArgsConstructor
public class AnswerReview {

    private Long questionId;
    private int number;
    private String text;
    private List<String> options;
    private int correctIndex;
    /** -1 = bỏ trống. */
    private int chosenIndex;
    private String topic;
    /** Đoạn văn dùng chung (nếu có) — cần cho câu đọc hiểu. */
    private String passage;

    public boolean isCorrect() {
        return chosenIndex == correctIndex;
    }

    public boolean isSkipped() {
        return chosenIndex < 0;
    }

    public String getCorrectLetter() {
        return letter(correctIndex);
    }

    public String getChosenLetter() {
        return chosenIndex < 0 ? "—" : letter(chosenIndex);
    }

    public String getCorrectText() {
        return optionAt(correctIndex);
    }

    public String getChosenText() {
        return optionAt(chosenIndex);
    }

    private String optionAt(int i) {
        return (options != null && i >= 0 && i < options.size()) ? options.get(i) : null;
    }

    private static String letter(int i) {
        return i < 0 ? "—" : String.valueOf((char) ('A' + i));
    }
}
