package com.pathstudy.domain;

public enum MaterialType {
    VIDEO("Video", "video"),
    DOCUMENT("Tài liệu", "file"),
    LINK("Liên kết", "link");

    private final String label;
    private final String icon;

    MaterialType(String label, String icon) {
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
