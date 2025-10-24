package com.aura.starter.data;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.aura.starter.model.Post;
import java.util.*;

public class AppRepository {
    private static AppRepository INSTANCE;
    private final MutableLiveData<List<Post>> postsLive = new MutableLiveData<>();

    private AppRepository(){
        List<Post> seed = generateMockPosts();
        postsLive.setValue(seed);
    }

    private List<Post> generateMockPosts() {
        List<Post> allPosts = new ArrayList<>();

        // 第一组：包含关键词的帖子（10个）
        List<Post> keywordPosts = new ArrayList<>();
        // 前5个帖子的图片从assets/images/加载
        keywordPosts.add(new Post(uuid(),"Fitness Enthusiast","Morning Workout Routine","Started my day with an intense fitness session. The outcome was amazing - burned 500 calories in just 45 minutes!","Fitness,Workout,Health", "img1"));
        keywordPosts.add(new Post(uuid(),"Diet Coach","Healthy Meal Prep","Preparing nutritious meals is key to a successful diet. This recipe uses fresh vegetables and lean proteins.","Diet,Recipe,MealPrep", "img2"));
        keywordPosts.add(new Post(uuid(),"Recipe Blogger","Protein Smoothie Bowl","Try this delicious recipe for a post-workout meal. The outcome is a satisfying and nutritious breakfast.","Recipe,Fitness,Diet", "img3"));
        keywordPosts.add(new Post(uuid(),"Wellness Guru","Intermittent Fasting Results","After 2 weeks of intermittent fasting, I've seen incredible outcomes in my energy levels and mental clarity.","Diet,Fitness,Wellness", "img4"));
        keywordPosts.add(new Post(uuid(),"Nutrition Expert","Balanced Diet Tips","A proper diet requires balance. Here's my recipe for sustainable healthy eating with great outcomes.","Diet,Recipe,Nutrition", "img5"));
        keywordPosts.add(new Post(uuid(),"Fitness Trainer","HIIT Workout Plan","This high-intensity fitness routine delivers amazing outcomes in just 20 minutes.","Fitness,HIIT,Workout", null));
        keywordPosts.add(new Post(uuid(),"Healthy Chef","Vegan Recipe Collection","Collection of delicious vegan recipes that support a plant-based diet and great fitness outcomes.","Recipe,Diet,Vegan", null));
        keywordPosts.add(new Post(uuid(),"Lifestyle Coach","Mindful Eating Guide","Mindful eating has transformed my diet approach. The outcomes speak for themselves.","Diet,Mindfulness,Health", null));
        keywordPosts.add(new Post(uuid(),"Wellness Blogger","Yoga and Fitness Combo","Combining yoga with strength training gives incredible fitness outcomes.","Fitness,Yoga,Wellness", null));
        keywordPosts.add(new Post(uuid(),"Health Advocate","Sugar-Free Recipe","This sugar-free recipe is perfect for maintaining a healthy diet and achieving fitness goals.","Recipe,Diet,Health", null));

        // 第二组：常规健康软件帖子（10个）
        List<Post> regularPosts1 = new ArrayList<>();
        regularPosts1.add(new Post(uuid(),"Health Walker","Daily Steps Challenge","Walking 10,000 steps daily has become my healthy habit. It's amazing how consistent small actions lead to great outcomes.","Walking,Health,Challenge", null));
        regularPosts1.add(new Post(uuid(),"Meditation Guide","Morning Mindfulness","Starting the day with 10 minutes of meditation sets a positive tone. The mental clarity outcome is worth it.","Meditation,Mindfulness,MentalHealth", null));
        regularPosts1.add(new Post(uuid(),"Sleep Expert","Quality Sleep Tips","7-8 hours of quality sleep is crucial for health. Here's how I optimize my sleep routine.","Sleep,Health,Wellness", null));
        regularPosts1.add(new Post(uuid(),"Hydration Coach","Water Intake Goal","Drinking enough water throughout the day makes a huge difference in energy levels and overall health.","Hydration,Health,Wellness", null));
        regularPosts1.add(new Post(uuid(),"Stress Manager","Work-Life Balance","Finding balance between work and personal life is key to mental health and wellbeing.","Stress,WorkLife,Balance", null));
        regularPosts1.add(new Post(uuid(),"Nutrition Student","Food Journal Benefits","Keeping a food journal has helped me understand my eating patterns and make better choices.","Nutrition,Journal,Health", null));
        regularPosts1.add(new Post(uuid(),"Fitness Newbie","Gym Introduction","Started going to the gym regularly. The environment and community make fitness more enjoyable.","Gym,Fitness,Community", null));
        regularPosts1.add(new Post(uuid(),"Wellness Seeker","Nature Walks","Regular nature walks help clear my mind and improve my mood significantly.","Nature,Walking,MentalHealth", null));
        regularPosts1.add(new Post(uuid(),"Health Blogger","Vitamin D Importance","Getting enough vitamin D through sunlight exposure has improved my overall health outcomes.","VitaminD,Sunlight,Health", null));
        regularPosts1.add(new Post(uuid(),"Lifestyle Enthusiast","Reading Habit","Reading before bed instead of screens has improved my sleep quality tremendously.","Reading,Habit,Sleep", null));

        // 第三组：常规健康软件帖子（10个）
        List<Post> regularPosts2 = new ArrayList<>();
        regularPosts2.add(new Post(uuid(),"Active Lifestyle","Cycling Adventures","Cycling through the city parks is my favorite way to stay active and explore.","Cycling,Active,Exploration", null));
        regularPosts2.add(new Post(uuid(),"Mindful Parent","Family Health Activities","Planning family activities that promote health and bonding time together.","Family,Health,Activities", null));
        regularPosts2.add(new Post(uuid(),"Yoga Practitioner","Flexibility Training","Regular yoga practice has improved my flexibility and reduced muscle tension.","Yoga,Flexibility,Wellness", null));
        regularPosts2.add(new Post(uuid(),"Health Tracker","Progress Photos","Documenting my health journey through progress photos keeps me motivated.","Progress,Health,Journey", null));
        regularPosts2.add(new Post(uuid(),"Nutrition Coach","Meal Planning","Weekly meal planning saves time and ensures I eat nutritious meals throughout the week.","MealPlanning,Nutrition,TimeManagement", null));
        regularPosts2.add(new Post(uuid(),"Fitness Community","Group Workouts","Joining group fitness classes has made exercising more social and enjoyable.","GroupFitness,Community,Social", null));
        regularPosts2.add(new Post(uuid(),"Wellness Writer","Gratitude Practice","Daily gratitude journaling has improved my mental health and overall outlook.","Gratitude,Journaling,MentalHealth", null));
        regularPosts2.add(new Post(uuid(),"Health Advocate","Preventive Care","Regular health check-ups and preventive care are essential for long-term wellness.","PreventiveCare,Health,Wellness", null));
        regularPosts2.add(new Post(uuid(),"Active Senior","Senior Fitness","Staying active as I age is important. Walking and light strength training keep me strong.","SeniorFitness,Active,Aging", null));
        regularPosts2.add(new Post(uuid(),"Lifestyle Blogger","Digital Detox","Taking breaks from digital devices has improved my focus and reduced eye strain.","DigitalDetox,Focus,Wellness", null));

        // 第四组：常规健康软件帖子（10个）
        List<Post> regularPosts3 = new ArrayList<>();
        regularPosts3.add(new Post(uuid(),"Plant Based","Plant-Based Journey","Transitioning to a plant-based diet has been rewarding for both health and environmental reasons.","PlantBased,Diet,Environment", null));
        regularPosts3.add(new Post(uuid(),"Mental Health Ally","Therapy Benefits","Regular therapy sessions have been instrumental in managing stress and improving mental health.","Therapy,MentalHealth,Wellness", null));
        regularPosts3.add(new Post(uuid(),"Fitness Model","Strength Training","Building muscle through consistent strength training has boosted my confidence and health.","StrengthTraining,Fitness,Confidence", null));
        regularPosts3.add(new Post(uuid(),"Health Researcher","Study on Sleep","Recent studies show that quality sleep is as important as diet and exercise for overall health.","Sleep,Research,Health", null));
        regularPosts3.add(new Post(uuid(),"Wellness Coach","Breathing Exercises","Deep breathing exercises help manage stress and improve focus throughout the day.","Breathing,Stress,Focus", null));
        regularPosts3.add(new Post(uuid(),"Nutrition Writer","Omega-3 Benefits","Including omega-3 rich foods in my diet has improved brain function and reduced inflammation.","Omega3,Nutrition,BrainHealth", null));
        regularPosts3.add(new Post(uuid(),"Active Family","Family Sports","Playing sports with my family keeps us all active and creates wonderful memories.","Family,Sports,Active", null));
        regularPosts3.add(new Post(uuid(),"Mind Body Coach","Meditation Practice","Daily meditation practice has enhanced my emotional intelligence and stress management.","Meditation,EmotionalIntelligence,Stress", null));
        regularPosts3.add(new Post(uuid(),"Health Podcaster","Podcast Episode","My latest podcast episode discusses the importance of work-life balance for mental health.","Podcast,WorkLife,MentalHealth", null));
        regularPosts3.add(new Post(uuid(),"Fitness Influencer","Marathon Training","Training for my first marathon has taught me discipline and the power of consistent effort.","Marathon,Fitness,Discipline", null));

        // 将所有帖子合并
        allPosts.addAll(keywordPosts);
        allPosts.addAll(regularPosts1);
        allPosts.addAll(regularPosts2);
        allPosts.addAll(regularPosts3);

        // 设置创建时间
        long now = System.currentTimeMillis();
        for (int i = 0; i < allPosts.size(); i++) {
            allPosts.get(i).createdAt = now - (long)(allPosts.size() - i) * 3600000L; // 每小时一个帖子
        }

        return allPosts;
    }

    private String uuid(){ return java.util.UUID.randomUUID().toString(); }

    public static synchronized AppRepository get(){
        if (INSTANCE == null) INSTANCE = new AppRepository();
        return INSTANCE;
    }

    public LiveData<List<Post>> posts(){ return postsLive; }

    public void addPost(Post p){
        List<Post> cur = new ArrayList<>(getOrEmpty());
        cur.add(0, p);
        postsLive.setValue(cur);
    }
    public void toggleLike(String id){
        for (Post p : getOrEmpty()) if (p.id.equals(id)){
            p.liked = !p.liked; p.likes += p.liked?1:-1;
            postsLive.setValue(new ArrayList<>(getOrEmpty()));
            return;
        }
    }
    public void toggleBookmark(String id){
        for (Post p : getOrEmpty()) if (p.id.equals(id)){
            p.bookmarked = !p.bookmarked;
            postsLive.setValue(new ArrayList<>(getOrEmpty()));
            return;
        }
    }
    public void setImage(String id, String uri){
        for (Post p : getOrEmpty()) if (p.id.equals(id)){
            p.imageUri = uri;
            postsLive.setValue(new ArrayList<>(getOrEmpty()));
            return;
        }
    }
    public void addComment(String id, String text){
        for (Post p : getOrEmpty()) if (p.id.equals(id)){
            if (p.comments == null) p.comments = new ArrayList<>();
            p.comments.add(text);
            postsLive.setValue(new ArrayList<>(getOrEmpty()));
            return;
        }
    }

    private List<Post> getOrEmpty(){
        List<Post> v = postsLive.getValue();
        return v == null ? new ArrayList<>() : v;
    }
}
