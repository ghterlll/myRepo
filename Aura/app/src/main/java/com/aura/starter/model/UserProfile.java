package com.aura.starter.model;

import java.io.Serializable;

public class UserProfile implements Serializable {
    public String name = "You";
    public String bio = "This user has not written a bio yet.";
    public String avatarUri;
    public String coverUri;
    public String linkInstagram = "https://instagram.com/";
    public String linkXhs = "https://www.xiaohongshu.com/";
    public int themeIndex = 0; // 0=orange,1=blue,2=purple,3=teal
}
