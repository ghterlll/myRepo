package com.aura.starter.data;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.aura.starter.model.Mission;
import java.util.*;

public class MissionRepository {
    private static MissionRepository INSTANCE;
    private final MutableLiveData<List<Mission>> missionsLive = new MutableLiveData<>();

    private MissionRepository(){
        List<Mission> list = new ArrayList<>();
        list.add(seed(-37.7963,144.9614,"UniMelb Old Arts","Take a wide-angle group photo in front of Old Arts",30));
        list.add(seed(-37.8076,144.9568,"QVM Night Market","Collect 3 different snacks and share a photo",20));
        list.add(seed(-37.8162,144.9690,"Hosier Lane Graffiti","Find the latest graffiti and upload a photo",25));
        list.add(seed(-37.8311,144.9802,"Royal Botanic Gardens Steps","Record 3km screenshot + photo",35));
        missionsLive.setValue(list);
    }
    private Mission seed(double lat,double lng,String t,String d,int pts){
        Mission m = new Mission(java.util.UUID.randomUUID().toString(), t, d, pts, lat, lng);
        m.target = 3; return m;
    }
    public static synchronized MissionRepository get(){
        if (INSTANCE==null) INSTANCE = new MissionRepository();
        return INSTANCE;
    }
    public LiveData<List<Mission>> missions(){ return missionsLive; }

    private List<Mission> getOrEmpty(){ return missionsLive.getValue()==null? new ArrayList<>() : missionsLive.getValue(); }
    private void push(){ missionsLive.setValue(new ArrayList<>(getOrEmpty())); }

    public Mission byId(String id){
        for (Mission m: getOrEmpty()) if (m.id.equals(id)) return m;
        return null;
    }
    public void increment(String id){
        Mission m = byId(id); if (m==null || m.done) return;
        m.progress = Math.min(m.target, m.progress+1);
        if (m.progress >= m.target) m.done = true;
        push();
    }
    public void setPhoto(String id, String uri){
        Mission m = byId(id); if (m==null) return;
        m.photoUri = uri;
        push();
    }
}
