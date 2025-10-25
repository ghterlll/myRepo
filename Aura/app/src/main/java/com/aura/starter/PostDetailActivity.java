package com.aura.starter;

import android.net.Uri;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aura.starter.data.AppRepository;
import com.aura.starter.model.Post;
import com.bumptech.glide.Glide;

public class PostDetailActivity extends AppCompatActivity {
    private Post post;
    private ImageButton btnLike, btnBookmark, btnBack;
    private ImageView img;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);

        post = (Post)getIntent().getSerializableExtra("post");

        TextView tvTitle = findViewById(R.id.tvTitle);
        TextView tvAuthor = findViewById(R.id.tvAuthor);
        TextView tvTags = findViewById(R.id.tvTags);
        TextView tvContent = findViewById(R.id.tvContent);
        TextView tvPostTime = findViewById(R.id.tvPostTime);
        img = findViewById(R.id.imgCover);

        btnBack = findViewById(R.id.btnBack);
        btnLike = findViewById(R.id.btnLikeDetail);
        btnBookmark = findViewById(R.id.btnBookmarkDetail);

        // Back button handler
        btnBack.setOnClickListener(v -> finish());

        btnLike.setOnClickListener(v -> { AppRepository.get().toggleLike(post.id); bind(); });
        btnBookmark.setOnClickListener(v -> { AppRepository.get().toggleBookmark(post.id); bind(); });

        tvTitle.setText(post.title);
        tvAuthor.setText(post.author);
        tvTags.setText("# " + (post.tags == null ? "" : post.tags));
        tvContent.setText(post.content);
        tvPostTime.setText("2 hours ago"); // TODO: Calculate actual time difference

        bind();
    }

    private void bind(){
        for (Post p : AppRepository.get().posts().getValue()){
            if (p.id.equals(post.id)){ post = p; break; }
        }
        btnLike.setImageResource(post.liked ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
        btnBookmark.setImageResource(post.bookmarked ? R.drawable.ic_bookmark_filled : R.drawable.ic_bookmark_outline);
        if (post.imageUri != null && !post.imageUri.isEmpty()){
            Glide.with(this).load(Uri.parse(post.imageUri)).placeholder(R.drawable.placeholder).into(img);
        } else {
            img.setImageResource(R.drawable.placeholder);
        }
    }
}
