package com.mobile.aura.constant;

import lombok.Getter;

@Getter
public enum GenderEnum {
    FEMALE(0, "female"),
    MALE(1, "male"),
    OTHER(2, "other");

    private final int code;
    private final String text;

    GenderEnum(int code, String text) {
        this.code = code;
        this.text = text;
    }

    public static Integer toCode(String text) {
        if (text == null) return null;
        String t = text.trim().toLowerCase();
        for (GenderEnum g : values()) if (g.text.equals(t)) return g.code;
        return null;
    }

    public static String toText(Integer code) {
        if (code == null) return null;
        for (GenderEnum g : values()) if (g.code == code) return g.text;
        return null;
    }
}
