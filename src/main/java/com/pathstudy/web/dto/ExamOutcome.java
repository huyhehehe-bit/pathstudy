package com.pathstudy.web.dto;

import com.pathstudy.domain.Exam;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ExamOutcome {
    private Exam exam;
    private int total;
    private int correct;
    private int score;
    private String level;
    private List<String> weakTopics;
    private String studyPlan;
}
