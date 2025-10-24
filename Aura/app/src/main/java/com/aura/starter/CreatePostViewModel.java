package com.aura.starter;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * 创建帖子的ViewModel，用于管理UI状态持久化
 */
public class CreatePostViewModel extends ViewModel {

    // 标题
    private final MutableLiveData<String> title = new MutableLiveData<>("");
    // 内容
    private final MutableLiveData<String> content = new MutableLiveData<>("");
    // 选中的标签
    private final MutableLiveData<List<String>> selectedTags = new MutableLiveData<>(new ArrayList<>());
    // 选中的图片路径
    private final MutableLiveData<String> selectedImagePath = new MutableLiveData<>("");

    // Getters
    public LiveData<String> getTitle() { return title; }
    public LiveData<String> getContent() { return content; }
    public LiveData<List<String>> getSelectedTags() { return selectedTags; }
    public LiveData<String> getSelectedImagePath() { return selectedImagePath; }

    private static final String TAG = "CreatePostViewModel";

    // Setters
    public void setTitle(String title) {
        Log.d(TAG, "setTitle: " + title);
        this.title.setValue(title);
    }

    public void setContent(String content) {
        Log.d(TAG, "setContent: " + content);
        this.content.setValue(content);
    }

    public void setSelectedTags(List<String> tags) {
        Log.d(TAG, "setSelectedTags: " + tags);
        this.selectedTags.setValue(new ArrayList<>(tags));
    }

    public void setSelectedImagePath(String path) {
        Log.d(TAG, "setSelectedImagePath: " + path);
        this.selectedImagePath.setValue(path);
    }

    // 添加标签
    public void addTag(String tag) {
        List<String> currentTags = selectedTags.getValue();
        if (currentTags == null) {
            currentTags = new ArrayList<>();
        }
        if (!currentTags.contains(tag)) {
            currentTags.add(tag);
            Log.d(TAG, "addTag: " + tag + ", current tags: " + currentTags);
            selectedTags.setValue(currentTags);
        }
    }

    // 移除标签
    public void removeTag(String tag) {
        List<String> currentTags = selectedTags.getValue();
        if (currentTags != null) {
            currentTags.remove(tag);
            Log.d(TAG, "removeTag: " + tag + ", current tags: " + currentTags);
            selectedTags.setValue(currentTags);
        }
    }

    // 清空所有状态
    public void clearAll() {
        Log.d(TAG, "clearAll");
        title.setValue("");
        content.setValue("");
        selectedTags.setValue(new ArrayList<>());
        selectedImagePath.setValue("");
    }
}
