package com.pathstudy.web.dto;

import com.pathstudy.domain.CourseModule;
import com.pathstudy.domain.Subject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class PathSummary {
    private Subject subject;
    private String level;
    private boolean hasPlacement;
    private int overallPercent;
    private int completedCount;
    private int totalCount;
    private List<ModuleCard> modules;
    private CourseModule currentModule;
    private String currentLessonTitle;

    /** Điểm bài test đầu vào */
    private Integer placementScore;
    /** Danh sách chủ đề cần cải thiện */
    private List<String> weakTopics;
    /** Danh sách chủ đề đã nắm vững */
    private List<String> masteredTopics;
    /** Lời khuyên/giáo trình chi tiết từ AI */
    private String studyPlan;
    /** Tiêu đề tóm tắt lộ trình học thích ứng */
    private String aiRoadmapHeadline;
    /** Nội dung phân tích cá nhân hoá từ AI */
    private String aiRoadmapSubtext;
    /** Số lượng module đã được miễn giảm/hoàn thành qua test */
    private int fastTrackedCount;

    public PathSummary(Subject subject, String level, boolean hasPlacement,
                       int overallPercent, int completedCount, int totalCount,
                       List<ModuleCard> modules, CourseModule currentModule,
                       String currentLessonTitle) {
        this(subject, level, hasPlacement, overallPercent, completedCount, totalCount,
             modules, currentModule, currentLessonTitle, null, List.of(), List.of(),
             null, null, null, 0);
    }
}
