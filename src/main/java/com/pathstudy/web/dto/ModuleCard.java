package com.pathstudy.web.dto;

import com.pathstudy.domain.CourseModule;
import com.pathstudy.domain.Exam;
import com.pathstudy.domain.ProgressStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
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

    public String getStepLabel() {
        return String.format("%02d", module.getOrderIndex());
    }

    public boolean isHasDetail() {
        return (sectionTitles != null && !sectionTitles.isEmpty())
                || (exams != null && !exams.isEmpty());
    }
}
