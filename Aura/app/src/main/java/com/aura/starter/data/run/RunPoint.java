package com.aura.starter.data.run;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class RunPoint {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public long sessionId;
    public double lat;
    public double lon;
    public int seq;
}
