package com.aura.starter.data.run;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class RunSession {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public long startTimeMillis;
    public long endTimeMillis;
    public double distanceMeters;
    public long durationMillis;
    public double avgPaceMinPerKm;
    public double kcal;
}
