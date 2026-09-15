package com.pathstudy.web.dto;

import com.pathstudy.domain.Subject;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class PlacementOutcome {
    private Subject subject;
    private int score;
    private String level;
    private List<String> strengths;
    private List<String> weaknesses;
    private int attemptNo;
    private int attemptsUsed;
    private int attemptsMax;
    private boolean bestUpdated;
}
