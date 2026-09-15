package com.pathstudy.domain;

/**
 * The skill dimensions a test measures. Placement and estimate tests report a
 * score per competency so the system can point at what to improve.
 */
public enum Competency {
    KNOWLEDGE("Kiến thức"),
    COMPREHENSION("Đọc hiểu"),
    ANALYSIS("Phân tích"),
    APPLICATION("Vận dụng");

    private final String label;

    Competency(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
