package com.pathstudy.domain;

/** Premium plans. Amounts in VND. */
public enum PremiumPlan {
    MONTHLY("Gói tháng", 99000, 30),
    YEARLY("Gói năm", 599000, 365);

    private final String label;
    private final int amount;
    private final int days;

    PremiumPlan(String label, int amount, int days) {
        this.label = label;
        this.amount = amount;
        this.days = days;
    }

    public String getLabel() {
        return label;
    }

    public int getAmount() {
        return amount;
    }

    public int getDays() {
        return days;
    }

    public static PremiumPlan fromCode(String code) {
        try {
            return PremiumPlan.valueOf(code);
        } catch (IllegalArgumentException | NullPointerException e) {
            return MONTHLY;
        }
    }
}
