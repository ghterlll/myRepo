package com.aura.starter.util;

import android.content.Context;
import android.content.SharedPreferences;

public class EnergyCalc {
    private static final String SP = "user_prefs";
    private static final String KEY_WEIGHT = "weight_kg";

    public static double readUserWeight(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(SP, Context.MODE_PRIVATE);
        return Double.longBitsToDouble(sp.getLong(KEY_WEIGHT, Double.doubleToLongBits(70.0)));
    }

    public static void saveUserWeight(Context ctx, double kg) {
        ctx.getSharedPreferences(SP, Context.MODE_PRIVATE)
                .edit().putLong(KEY_WEIGHT, Double.doubleToLongBits(kg)).apply();
    }

    public static double kcalFromRunKm(double weightKg, double km) {
        return 1.036 * weightKg * km;
    }

    public static double kcalFromSteps(double weightKg, int steps, double stepLenMeters) {
        double km = steps * stepLenMeters / 1000.0;
        return 0.67 * weightKg * km;
    }
}
