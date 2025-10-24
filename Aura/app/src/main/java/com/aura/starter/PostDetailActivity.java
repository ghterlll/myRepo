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
    private ImageButton btnLike, btnBookmark, btnPick;
    private ImageView img;
    private CommentsAdapter commentsAdapter;

    private final ActivityResultLauncher<String> pickImage = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null){
                    AppRepository.get().setImage(post.id, uri.toString());
                    bind();
                }
            }
    );

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);

        post = (Post)getIntent().getSerializableExtra("post");

        TextView tvTitle = findViewById(R.id.tvTitle);
        TextView tvAuthor = findViewById(R.id.tvAuthor);
        TextView tvTags = findViewById(R.id.tvTags);
        TextView tvContent = findViewById(R.id.tvContent);
        img = findViewById(R.id.imgCover);

        btnLike = findViewById(R.id.btnLikeDetail);
        btnBookmark = findViewById(R.id.btnBookmarkDetail);
        btnPick = findViewById(R.id.btnPickImage);

        RecyclerView rv = findViewById(R.id.recyclerComments);
        rv.setLayoutManager(new LinearLayoutManager(this));
        commentsAdapter = new CommentsAdapter();
        rv.setAdapter(commentsAdapter);

        EditText et = findViewById(R.id.etComment);
        ImageButton send = findViewById(R.id.btnSendComment);
        send.setOnClickListener(v -> {
            String t = et.getText().toString().trim();
            if (!t.isEmpty()){
                AppRepository.get().addComment(post.id, t);
                et.setText("");
                bind();
            }
        });

        btnPick.setOnClickListener(v -> pickImage.launch("image/*"));
        btnLike.setOnClickListener(v -> { AppRepository.get().toggleLike(post.id); bind(); });
        btnBookmark.setOnClickListener(v -> { AppRepository.get().toggleBookmark(post.id); bind(); });

        tvTitle.setText(post.title);
        tvAuthor.setText("by " + post.author);
        tvTags.setText("# " + (post.tags == null ? "" : post.tags));
        tvContent.setText(post.content);

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
        commentsAdapter.submit(post.comments);
    }
}
