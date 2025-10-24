package com.aura.starter.model;

public class Mission implements java.io.Serializable {
    public String id, title, desc;
    public int points;
    public double lat, lng;
    public boolean done;
    public int progress = 0;
    public int target = 3;          // default 3 times
    public String photoUri;         // user uploaded photo

    public Mission(String id, String title, String desc, int points, double lat, double lng){
        this.id=id; this.title=title; this.desc=desc; this.points=points; this.lat=lat; this.lng=lng;
    }
}
