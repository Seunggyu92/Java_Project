package com.volunteerintegration.mypage;

public enum RecordType {
    VOLUNTEER("봉사"),
    BLOOD("헌혈"),
    DONATION("기부");

    private final String label;

    RecordType(String label) {
        this.label = label;
    }

    public String getCode() {
        return name();
    }

    public String getLabel() {
        return label;
    }

    public static RecordType fromCode(String code) {
        for (RecordType t : values()) {
            if (t.name().equalsIgnoreCase(code)) {
                return t;
            }
        }
        return null;
    }
}
