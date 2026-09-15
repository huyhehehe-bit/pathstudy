package com.pathstudy.web.dto;

import com.pathstudy.domain.CourseModule;
import com.pathstudy.domain.Subject;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
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
}
