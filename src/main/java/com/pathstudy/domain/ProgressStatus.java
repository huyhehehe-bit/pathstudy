package com.pathstudy.domain;

public enum ProgressStatus {
    LOCKED("Chưa bắt đầu"),
    IN_PROGRESS("Đang học"),
    COMPLETED("Hoàn thành");

    private final String label;

    ProgressStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
