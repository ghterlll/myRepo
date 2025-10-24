package com.aura.starter;

public class DrinkType {
    private final String name;
    private final int iconRes;

    public DrinkType(String name, int iconRes) {
        this.name = name;
        this.iconRes = iconRes;
    }

    public String getName() { return name; }
    public int getIconRes() { return iconRes; }
}
