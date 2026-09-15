package com.pathstudy.web.dto;

import com.pathstudy.domain.CourseModule;
import com.pathstudy.domain.EstimateResult;
import com.pathstudy.domain.Lesson;
import com.pathstudy.domain.LessonSection;
import com.pathstudy.domain.ProgressStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.Set;

@Getter
@AllArgsConstructor
public class ModuleDetail {
    private CourseModule module;
    private Lesson lesson;
    private List<LessonSection> sections;
    private Set<Long> bookmarkedIds;
    private boolean hasEstimate;
    private ProgressStatus status;
    private EstimateResult lastEstimate;
    private CourseModule nextModule;
}
