package com.pathstudy.web.dto;

import com.pathstudy.domain.CourseModule;
import com.pathstudy.domain.Exam;
import com.pathstudy.domain.ProgressStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class ModuleCard {
    private CourseModule module;
    private ProgressStatus status;
    private int percent;
    private boolean current;
    private boolean locked;
    private String lessonTitle;

    /** Tên các phần trong bài của chương (lý thuyết, ví dụ, tự đánh giá...) —
        để trang lộ trình bung ra xem được chương gồm những gì. */
    private List<String> sectionTitles;

    /** Đề luyện dùng được cho chương này. Exam KHÔNG gắn FK tới module, chỉ có
        subject + grade, nên đây là đề cùng khối với chương. */
    private List<Exam> exams;

    /** Đánh dấu cá nhân hoá theo bài test đầu vào */
    private boolean weakTopic;
    private boolean masteredByPlacement;
    private String adaptiveBadge;
    private String adaptiveBadgeClass;

    public ModuleCard(CourseModule module, ProgressStatus status, int percent,
                      boolean current, boolean locked, String lessonTitle,
                      List<String> sectionTitles, List<Exam> exams) {
        this(module, status, percent, current, locked, lessonTitle, sectionTitles, exams,
                false, false, null, null);
    }

    public String getStepLabel() {
        return String.format("%02d", module.getOrderIndex());
    }

    public boolean isHasDetail() {
        return (sectionTitles != null && !sectionTitles.isEmpty())
                || (exams != null && !exams.isEmpty());
    }
}
