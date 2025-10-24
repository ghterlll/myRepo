package com.aura.starter.data;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.aura.starter.model.UserProfile;

public class ProfileRepository {
    private static ProfileRepository INSTANCE;
    private final MutableLiveData<UserProfile> live = new MutableLiveData<>();
    private final SharedPreferences sp;
    private static final String KEY = "profile";

    private ProfileRepository(Context ctx){
        sp = ctx.getSharedPreferences("profile_prefs", Context.MODE_PRIVATE);
        UserProfile p = new UserProfile();
        p.name = sp.getString("name", p.name);
        p.bio = sp.getString("bio", p.bio);
        p.avatarUri = sp.getString("avatar", null);
        p.coverUri = sp.getString("cover", null);
        p.linkInstagram = sp.getString("ig", p.linkInstagram);
        p.linkXhs = sp.getString("xhs", p.linkXhs);
        p.themeIndex = sp.getInt("theme", 0);
        live.setValue(p);
    }

    public static synchronized ProfileRepository get(Context ctx){
        if (INSTANCE==null) INSTANCE = new ProfileRepository(ctx.getApplicationContext());
        return INSTANCE;
    }

    public LiveData<UserProfile> profile(){ return live; }

    private void save(){
        UserProfile p = live.getValue(); if (p==null) return;
        sp.edit()
            .putString("name", p.name)
            .putString("bio", p.bio)
            .putString("avatar", p.avatarUri)
            .putString("cover", p.coverUri)
            .putString("ig", p.linkInstagram)
            .putString("xhs", p.linkXhs)
            .putInt("theme", p.themeIndex)
            .apply();
    }

    public void setAvatar(String uri){ UserProfile p = live.getValue(); if(p==null) return; p.avatarUri=uri; live.setValue(p); save(); }
    public void setCover(String uri){ UserProfile p = live.getValue(); if(p==null) return; p.coverUri=uri; live.setValue(p); save(); }
    public void setBio(String bio){ UserProfile p = live.getValue(); if(p==null) return; p.bio=bio; live.setValue(p); save(); }
    public void setName(String name){ UserProfile p = live.getValue(); if(p==null) return; p.name=name; live.setValue(p); save(); }
    public void setLinks(String ig, String xhs){ UserProfile p = live.getValue(); if(p==null) return; p.linkInstagram=ig; p.linkXhs=xhs; live.setValue(p); save(); }
    public void setTheme(int idx){ UserProfile p = live.getValue(); if(p==null) return; p.themeIndex=idx; live.setValue(p); save(); }
}
