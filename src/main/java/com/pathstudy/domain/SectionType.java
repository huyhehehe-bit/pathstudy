package com.pathstudy.domain;

/**
 * A module centres on one main work/topic surrounded by supporting tools.
 * Each supporting tool is a section of a lesson.
 */
public enum SectionType {
    READING("Đọc & hiểu tác phẩm", "book"),
    THEME("Chủ đề & nội dung", "target"),
    IMAGERY("Hình ảnh, biểu tượng", "image"),
    TECHNIQUE("Biện pháp nghệ thuật", "sparkles"),
    CONTEXT("Bối cảnh", "clock"),
    GUIDE("Hướng dẫn phân tích", "compass"),
    FRAMEWORK("Framework viết đoạn văn", "layout"),
    EXAMPLE("Ví dụ bài làm", "file"),
    VOCAB("Từ vựng & khái niệm", "book-open");

    private final String label;
    private final String icon;

    SectionType(String label, String icon) {
        this.label = label;
        this.icon = icon;
    }

    public String getLabel() {
        return label;
    }

    public String getIcon() {
        return icon;
    }
}
