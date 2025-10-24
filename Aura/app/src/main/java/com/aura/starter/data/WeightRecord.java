package com.aura.starter.data;

/**
 * Legacy weight record model (no longer used with Room DB)
 * Kept for compatibility with old code
 */
public class WeightRecord {
    public long id;

    public long dateMillis;
    public float kg;

    public WeightRecord(long dateMillis, float kg) {
        this.dateMillis = dateMillis;
        this.kg = kg;
    }
}
