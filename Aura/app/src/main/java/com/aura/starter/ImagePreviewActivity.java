package com.aura.starter;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import android.widget.ImageView;

/**
 * 图片全屏预览Activity
 * 支持图片缩放和平移查看
 */
public class ImagePreviewActivity extends AppCompatActivity {

    private ImageView photoView;
    private String imagePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 设置全屏
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
            View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        );

        setContentView(R.layout.activity_image_preview);

        photoView = findViewById(R.id.photoView);
        Intent intent = getIntent();
        imagePath = intent.getStringExtra("image_path");

        if (imagePath != null && !imagePath.isEmpty()) {
            loadImage();
        } else {
            Toast.makeText(this, "图片路径无效", Toast.LENGTH_SHORT).show();
            finish();
        }

        // 点击关闭
        findViewById(R.id.rootLayout).setOnClickListener(v -> finish());

        // 设置PhotoView的点击监听器，点击时关闭Activity
        photoView.setOnClickListener(v -> finish());
    }

    private void loadImage() {
        try {
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
            if (bitmap != null) {
                Glide.with(this)
                    .load(bitmap)
                    .into(photoView);
            } else {
                Toast.makeText(this, "图片加载失败", Toast.LENGTH_SHORT).show();
                finish();
            }
        } catch (Exception e) {
            Toast.makeText(this, "图片加载出错", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
