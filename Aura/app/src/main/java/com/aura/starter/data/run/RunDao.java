package com.aura.starter.data.run;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface RunDao {
    @Insert long insertSession(RunSession s);
    @Insert void insertPoints(List<RunPoint> pts);

    @Query("SELECT * FROM RunSession ORDER BY startTimeMillis DESC")
    List<RunSession> getAllSessions();

    @Query("SELECT * FROM RunPoint WHERE sessionId = :sid ORDER BY seq ASC")
    List<RunPoint> getPointsForSession(long sid);
}
