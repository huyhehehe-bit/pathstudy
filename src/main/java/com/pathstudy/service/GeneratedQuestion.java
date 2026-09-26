package com.pathstudy.service;

import java.util.List;

/**
 * Một câu hỏi trắc nghiệm do AI (Gemini) sinh ra theo form giáo viên nhập.
 * competency là tên enum {@link com.pathstudy.domain.Competency}
 * (KNOWLEDGE/COMPREHENSION/ANALYSIS/APPLICATION); caller tự map + kiểm tra.
 */
public record GeneratedQuestion(String text, List<String> options, int correctIndex,
                                String topic, String competency) {
}
