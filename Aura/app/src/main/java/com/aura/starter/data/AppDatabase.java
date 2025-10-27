package com.aura.starter.data;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import com.aura.starter.data.run.RunDao;
import com.aura.starter.data.run.RunPoint;
import com.aura.starter.data.run.RunSession;

@Database(entities = {RunSession.class, RunPoint.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase INSTANCE;
    public abstract RunDao runDao();
    public static AppDatabase get(Context ctx) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(ctx.getApplicationContext(),
                            AppDatabase.class, "aura.db").build();
                }
            }
        }
        return INSTANCE;
    }
}
