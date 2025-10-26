package com.aura.starter;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.aura.starter.model.Post;
import com.aura.starter.network.FileRepository;
import com.aura.starter.network.PostRepository;
import com.aura.starter.network.models.FileUploadResponse;
import com.aura.starter.network.models.MediaItem;
import com.aura.starter.network.models.PostCreateRequest;
import com.aura.starter.util.PermissionManager;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CreateFragment extends Fragment {
    private static final String TAG = "CreateFragment";
    private static final String PREF_DRAFT = "create_post_draft";

    private FeedViewModel feedVm;
    private CreatePostViewModel createVm;
    private EditText etTitle, etContent;
    private TextInputLayout tilTitle, tilContent;
    private TextView tagFitness, tagDiet, tagRecipe, tagPlan, tagOutcome;
    private ImageView imgPreview, iconCamera;
    private Button btnPublish;
    private ImageButton btnDeleteImage, btnBack;
    private LinearLayout layoutTags;
    private TextView tvSelectedTags;

    private String selectedImagePath;
    private List<String> selectedTags = new ArrayList<>();
    private boolean isDraftLoaded = false;
    private RequestManager glideRequestManager;
    private Uri cameraImageUri;
    private PermissionManager permissionManager;

    // 预定义标签
    private final List<String> predefinedTags = Arrays.asList("fitness", "diet", "recipe", "plan", "outcome");

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // 图片选择器
    private final ActivityResultLauncher<String> imagePicker = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    handleSelectedImage(uri);
                }
            }
    );

    // 相机
    private final ActivityResultLauncher<Uri> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicture(), success -> {
                if (success && cameraImageUri != null) {
                    handleSelectedImage(cameraImageUri);
                }
            }
    );

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate called");
        permissionManager = new PermissionManager(this);
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView called");
        View v = inflater.inflate(R.layout.fragment_create, container, false);
        feedVm = new ViewModelProvider(requireActivity()).get(FeedViewModel.class);
        createVm = new ViewModelProvider(requireActivity()).get(CreatePostViewModel.class);
        glideRequestManager = Glide.with(this);

        Log.d(TAG, "Initializing views...");
        initializeViews(v);
        setupValidation();
        setupTags();
        setupImagePicker();

        Log.d(TAG, "Setting up ViewModel observers...");
        setupViewModelObservers();

        // 延迟一点时间再加载草稿，确保chips已经完全初始化
        mainHandler.postDelayed(() -> {
            Log.d(TAG, "Delayed draft loading...");
        loadDraft();
        }, 100);

        Log.d(TAG, "Current ViewModel state - Title: " + createVm.getTitle().getValue() +
              ", Content: " + createVm.getContent().getValue() +
              ", Tags: " + createVm.getSelectedTags().getValue() +
              ", Image: " + createVm.getSelectedImagePath().getValue());

        return v;
    }

    private void initializeViews(View v) {
        etTitle = v.findViewById(R.id.etTitle);
        etContent = v.findViewById(R.id.etContent);
        tilTitle = v.findViewById(R.id.tilTitle);
        tilContent = v.findViewById(R.id.tilContent);

        // Tag TextViews
        tagFitness = v.findViewById(R.id.tagFitness);
        tagDiet = v.findViewById(R.id.tagDiet);
        tagRecipe = v.findViewById(R.id.tagRecipe);
        tagPlan = v.findViewById(R.id.tagPlan);
        tagOutcome = v.findViewById(R.id.tagOutcome);

        imgPreview = v.findViewById(R.id.imgPreview);
        iconCamera = v.findViewById(R.id.iconCamera);
        btnPublish = v.findViewById(R.id.btnPublish);
        btnBack = v.findViewById(R.id.btnBack);
        layoutTags = v.findViewById(R.id.layoutTags);
        tvSelectedTags = v.findViewById(R.id.tvSelectedTags);

        // Back button
        btnBack.setOnClickListener(v1 -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        // Delete image button
        btnDeleteImage = v.findViewById(R.id.btnDeleteImage);
        btnDeleteImage.setOnClickListener(this::deleteSelectedImage);

        // Image preview click listener
        imgPreview.setOnClickListener(this::onImagePreviewClick);

        btnPublish.setOnClickListener(this::publishPost);

        // Clear button listeners
        tilTitle.setEndIconOnClickListener(view -> {
            etTitle.setText("");
            createVm.setTitle("");
        });

        tilContent.setEndIconOnClickListener(view -> {
            etContent.setText("");
            createVm.setContent("");
        });
    }

    private void setupValidation() {
        etTitle.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // 更新ViewModel状态
                createVm.setTitle(s.toString());
                updateTitleValidation();
                autoSaveDraft();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        etContent.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // 更新ViewModel状态
                createVm.setContent(s.toString());
                updateContentValidation();
                autoSaveDraft();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        updateValidationDisplay();
    }

    private void setupTags() {
        Log.d(TAG, "setupTags called");

        View.OnClickListener tagClickListener = v -> {
            TextView tag = (TextView) v;
            boolean isSelected = tag.isSelected();
            tag.setSelected(!isSelected);

            String tagText = tag.getText().toString();
            if (!isSelected) {
                createVm.addTag(tagText);
            } else {
                createVm.removeTag(tagText);
            }
            autoSaveDraft();
        };

        tagFitness.setOnClickListener(tagClickListener);
        tagDiet.setOnClickListener(tagClickListener);
        tagRecipe.setOnClickListener(tagClickListener);
        tagPlan.setOnClickListener(tagClickListener);
        tagOutcome.setOnClickListener(tagClickListener);

        Log.d(TAG, "setupTags completed");
    }

    private void setupImagePicker() {
        // 图片选择逻辑现在通过onImagePreviewClick处理
        updateImagePreviewState();
    }

    /**
     * Update image preview area state
     */
    private void updateImagePreviewState() {
        if (selectedImagePath != null && !selectedImagePath.isEmpty()) {
            // State 2: Show thumbnail and delete button
            btnDeleteImage.setVisibility(View.VISIBLE);
            iconCamera.setVisibility(View.GONE);
            imgPreview.setBackgroundResource(R.drawable.bg_image_with_delete);
        } else {
            // State 1: Show camera icon
            btnDeleteImage.setVisibility(View.GONE);
            iconCamera.setVisibility(View.VISIBLE);
            imgPreview.setBackgroundResource(R.drawable.bg_add_image);
        }
    }

    private void setupViewModelObservers() {
        Log.d(TAG, "setupViewModelObservers called");

        // 观察标签状态变化
        createVm.getSelectedTags().observe(getViewLifecycleOwner(), tags -> {
            Log.d(TAG, "Tags observer triggered, new tags: " + tags + ", current selectedTags: " + selectedTags);
            selectedTags.clear();
            selectedTags.addAll(tags);
            Log.d(TAG, "After updating selectedTags: " + selectedTags);
            updateTagsDisplay();
            updateSelectedTags(); // 关键：更新chips的选中状态
            updateValidationDisplay();
        });

        // 观察标题变化
        createVm.getTitle().observe(getViewLifecycleOwner(), title -> {
            Log.d(TAG, "Title observer triggered, new title: " + title);
            if (!etTitle.getText().toString().equals(title)) {
                etTitle.setText(title);
            }
        });

        // 观察内容变化
        createVm.getContent().observe(getViewLifecycleOwner(), content -> {
            Log.d(TAG, "Content observer triggered, new content: " + content);
            if (!etContent.getText().toString().equals(content)) {
                etContent.setText(content);
            }
        });

        // 观察图片路径变化
        createVm.getSelectedImagePath().observe(getViewLifecycleOwner(), path -> {
            Log.d(TAG, "Image path observer triggered, new path: " + path);
            selectedImagePath = path;
            if (path != null && !path.isEmpty()) {
                try {
                    Bitmap bitmap = BitmapFactory.decodeFile(path);
                    if (bitmap != null) {
                        glideRequestManager.load(bitmap).into(imgPreview);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Failed to load image from ViewModel", e);
                }
            } else {
                imgPreview.setImageResource(R.drawable.placeholder);
            }
            // 更新图片预览区域状态
            updateImagePreviewState();
        });
    }

    private void showImagePickerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Select Image Source");
        builder.setItems(new String[]{"Gallery", "Camera"}, (dialog, which) -> {
            if (which == 0) {
                // Gallery
                imagePicker.launch("image/*");
            } else {
                // Camera - check permission first
                String cameraPermission = android.Manifest.permission.CAMERA;
                permissionManager.requestPermission(cameraPermission, new PermissionManager.PermissionCallback() {
                    @Override
                    public void onGranted(String permission) {
                        // Permission granted
                        startCameraCapture();
                    }

                    @Override
                    public void onDenied(String permission) {
                        // Permission denied
                        Toast.makeText(requireContext(),
                            "Camera permission is required. Please grant it in settings",
                            Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
        builder.show();
    }

    private File createImageFile() throws IOException {
        String timeStamp = String.valueOf(System.currentTimeMillis());
        String imageFileName = "POST_" + timeStamp + "_";
        File storageDir = requireContext().getExternalFilesDir("Pictures");
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    private void startCameraCapture() {
        try {
            File imageFile = createImageFile();
            if (imageFile != null) {
                selectedImagePath = imageFile.getAbsolutePath();
                cameraImageUri = FileProvider.getUriForFile(
                    requireContext(),
                    "com.aura.starter.fileprovider",
                    imageFile
                );
                cameraLauncher.launch(cameraImageUri);
            }
        } catch (IOException e) {
            Toast.makeText(requireContext(), "Cannot create image file", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 处理图片预览区域的点击事件
     * 实现三种状态切换：
     * 1. 没选图片时显示+号 -> 点击选择图片
     * 2. 选完图片后显示缩略图 -> 点击放大显示原图
     * 3. 放大显示时点击关闭回到状态2
     */
    private void onImagePreviewClick(View v) {
        if (selectedImagePath == null || selectedImagePath.isEmpty()) {
            // 状态1：没选图片时显示+号，点击选择图片
            showImagePickerDialog();
        } else {
            // 状态2：选完图片后显示缩略图，点击放大显示原图
            showImageFullScreen();
        }
    }

    /**
     * 显示图片全屏预览
     */
    private void showImageFullScreen() {
        if (selectedImagePath != null && !selectedImagePath.isEmpty()) {
            Intent intent = new Intent(requireContext(), ImagePreviewActivity.class);
            intent.putExtra("image_path", selectedImagePath);
            startActivity(intent);
        }
    }

    /**
     * 删除选中的图片
     */
    private void deleteSelectedImage(View v) {
        Log.d(TAG, "Deleting selected image");
        selectedImagePath = null;
        cameraImageUri = null;

        // 更新ViewModel状态
        createVm.setSelectedImagePath("");

        // 更新UI状态
        updateImagePreviewState();

        // 保存草稿
        autoSaveDraft();
    }

    private void handleSelectedImage(Uri uri) {
        try {
            // Save URI as string path (let Glide handle the loading and caching)
            selectedImagePath = uri.toString();

            // Update ViewModel state
            createVm.setSelectedImagePath(selectedImagePath);

            // Load image with Glide (modern approach - no manual compression)
            glideRequestManager
                .load(uri)
                .centerCrop()
                .into(imgPreview);

            // Update image preview state
            updateImagePreviewState();

            autoSaveDraft();
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Image processing failed", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error handling selected image", e);
        }
    }

    private File saveCompressedImage(Bitmap bitmap) throws IOException {
        File tempFile = new File(requireContext().getCacheDir(), "post_image_" + System.currentTimeMillis() + ".jpg");
        FileOutputStream fos = new FileOutputStream(tempFile);
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos);
        fos.close();
        return tempFile;
    }

    /**
     * 安全地从URI解码图片，防止内存溢出
     */
    private Bitmap decodeSampledBitmapFromUri(Uri uri, int reqWidth, int reqHeight) {
        try {
            // 首先获取图片尺寸
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;

            BitmapFactory.decodeStream(
                requireContext().getContentResolver().openInputStream(uri),
                null,
                options
            );

            // 计算采样率
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
            options.inJustDecodeBounds = false;

            // 解码图片
            return BitmapFactory.decodeStream(
                requireContext().getContentResolver().openInputStream(uri),
                null,
                options
            );
        } catch (Exception e) {
            Log.e(TAG, "Error decoding bitmap from uri", e);
            return null;
        }
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    private void updateTitleValidation() {
        String title = etTitle.getText().toString().trim();
        String[] words = title.split("\\s+");
        int wordCount = words.length;

        tilTitle.setHelperText(wordCount + " words");
        tilTitle.setError(title.isEmpty() ? "Title cannot be empty" : null);
    }

    private void updateContentValidation() {
        String content = etContent.getText().toString().trim();
        String[] words = content.split("\\s+");
        int wordCount = words.length;

        tilContent.setHelperText(wordCount + " words (at least 3)");
        tilContent.setError(wordCount < 3 ? "Content must have at least 3 words" : null);
    }

    private void updateTagsDisplay() {
        // 更新标签显示区域
        layoutTags.setVisibility(selectedTags.isEmpty() ? View.GONE : View.VISIBLE);
        if (tvSelectedTags != null) {
            tvSelectedTags.setText(String.join(", ", selectedTags));
        }
    }

    private void updateValidationDisplay() {
        boolean isValid = isFormValid();
        btnPublish.setEnabled(isValid);
    }

    private boolean isFormValid() {
        String title = etTitle.getText().toString().trim();
        String content = etContent.getText().toString().trim();

        String[] contentWords = content.split("\\s+");

        return !title.isEmpty() &&
               contentWords.length >= 3 &&
               !selectedTags.isEmpty();
    }

    private void autoSaveDraft() {
        if (isDraftLoaded) {
            Log.d(TAG, "autoSaveDraft skipped because isDraftLoaded is true");
            return; // 避免覆盖刚加载的草稿
        }

        Log.d(TAG, "autoSaveDraft called");
        SharedPreferences prefs = requireContext().getSharedPreferences(PREF_DRAFT, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        // 从ViewModel获取当前状态
        String title = createVm.getTitle().getValue() != null ? createVm.getTitle().getValue() : "";
        String content = createVm.getContent().getValue() != null ? createVm.getContent().getValue() : "";
        List<String> tags = createVm.getSelectedTags().getValue() != null ? createVm.getSelectedTags().getValue() : new ArrayList<>();
        String imagePath = createVm.getSelectedImagePath().getValue() != null ? createVm.getSelectedImagePath().getValue() : "";

        Log.d(TAG, "Saving to draft - title: '" + title + "', content: '" + content + "', tags: " + tags + ", imagePath: '" + imagePath + "'");

        editor.putString("title", title);
        editor.putString("content", content);
        editor.putString("selectedTags", String.join(",", tags));
        editor.putString("imagePath", imagePath);

        editor.apply();
        Log.d(TAG, "Draft saved successfully");
    }

    private void loadDraft() {
        Log.d(TAG, "loadDraft called");
        SharedPreferences prefs = requireContext().getSharedPreferences(PREF_DRAFT, Context.MODE_PRIVATE);

        String title = prefs.getString("title", "");
        String content = prefs.getString("content", "");
        String tags = prefs.getString("selectedTags", "");
        String imagePath = prefs.getString("imagePath", "");

        Log.d(TAG, "Draft data - title: '" + title + "', content: '" + content + "', tags: '" + tags + "', imagePath: '" + imagePath + "'");

        if (!title.isEmpty() || !content.isEmpty() || !tags.isEmpty() || !imagePath.isEmpty()) {
            Log.d(TAG, "Loading draft data into ViewModel");
            // 设置ViewModel状态，UI会通过观察者自动更新
            createVm.setTitle(title);
            createVm.setContent(content);
            if (!tags.isEmpty()) {
                List<String> tagList = Arrays.asList(tags.split(","));
                Log.d(TAG, "Setting tags from draft: " + tagList);
                createVm.setSelectedTags(tagList);
            }
            if (!imagePath.isEmpty()) {
                createVm.setSelectedImagePath(imagePath);
                // 更新图片预览区域状态
                updateImagePreviewState();
            }

            isDraftLoaded = true;
            Log.d(TAG, "Draft loaded successfully");
        } else {
            Log.d(TAG, "No draft data to load");
        }
    }

    private void updateSelectedTags() {
        Log.d(TAG, "updateSelectedTags called, selectedTags: " + selectedTags);

        tagFitness.setSelected(selectedTags.contains("fitness"));
        tagDiet.setSelected(selectedTags.contains("diet"));
        tagRecipe.setSelected(selectedTags.contains("recipe"));
        tagPlan.setSelected(selectedTags.contains("plan"));
        tagOutcome.setSelected(selectedTags.contains("outcome"));
    }

    private void publishPost(View v) {
        if (!isFormValid()) {
            Toast.makeText(requireContext(), "Please complete all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get latest state from ViewModel
        String title = createVm.getTitle().getValue() != null ? createVm.getTitle().getValue() : "";
        String content = createVm.getContent().getValue() != null ? createVm.getContent().getValue() : "";
        String imagePath = createVm.getSelectedImagePath().getValue();

        // Disable publish button to prevent double submission
        btnPublish.setEnabled(false);
        btnPublish.setText("Publishing...");

        // Step 1: Upload image first (if exists)
        if (imagePath != null && !imagePath.isEmpty()) {
            uploadImageThenCreatePost(title, content, imagePath);
        } else {
            // No image, create post directly
            createPostWithMedia(title, content, null);
        }
    }

    /**
     * Upload image first, then create post with image URL
     */
    private void uploadImageThenCreatePost(String title, String content, String imagePath) {
        FileRepository fileRepo = FileRepository.getInstance();

        // Convert image path to File
        File imageFile = null;
        try {
            if (imagePath.startsWith("content://") || imagePath.startsWith("file://")) {
                // URI path - need to convert to file
                Uri uri = Uri.parse(imagePath);
                imageFile = uriToFile(uri);
            } else {
                // File path
                imageFile = new File(imagePath);
            }

            if (imageFile == null || !imageFile.exists()) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Image file not found", Toast.LENGTH_SHORT).show();
                    resetPublishButton();
                });
                return;
            }

            final File finalImageFile = imageFile;
            fileRepo.uploadPostImage(finalImageFile, new FileRepository.ResultCallback<FileUploadResponse>() {
                @Override
                public void onSuccess(FileUploadResponse response) {
                    // Image uploaded successfully, now create post with image URL
                    String imageUrl = response.getUrl();
                    createPostWithMedia(title, content, imageUrl);
                }

                @Override
                public void onError(String message) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Failed to upload image: " + message, Toast.LENGTH_SHORT).show();
                        resetPublishButton();
                    });
                }
            });
        } catch (Exception e) {
            requireActivity().runOnUiThread(() -> {
                Toast.makeText(requireContext(), "Failed to process image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                resetPublishButton();
            });
        }
    }

    /**
     * Convert URI to File
     */
    private File uriToFile(Uri uri) {
        try {
            // Determine file extension from MIME type or URI
            String extension = getFileExtensionFromUri(uri);

            // Create temp file with correct extension
            File tempFile = File.createTempFile("upload_", extension, requireContext().getCacheDir());

            // Copy URI content to temp file
            java.io.InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
            FileOutputStream outputStream = new FileOutputStream(tempFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            inputStream.close();
            outputStream.close();

            return tempFile;
        } catch (Exception e) {
            Log.e(TAG, "Failed to convert URI to file", e);
            return null;
        }
    }

    /**
     * Get file extension from URI based on MIME type
     */
    private String getFileExtensionFromUri(Uri uri) {
        // Try to get MIME type from content resolver
        String mimeType = requireContext().getContentResolver().getType(uri);

        if (mimeType != null) {
            switch (mimeType) {
                case "image/jpeg":
                    return ".jpg";
                case "image/png":
                    return ".png";
                case "image/gif":
                    return ".gif";
                case "image/webp":
                    return ".webp";
            }
        }

        // Fallback: try to get extension from URI path
        String path = uri.getPath();
        if (path != null) {
            if (path.toLowerCase().endsWith(".png")) return ".png";
            if (path.toLowerCase().endsWith(".gif")) return ".gif";
            if (path.toLowerCase().endsWith(".webp")) return ".webp";
        }

        // Default to jpg
        return ".jpg";
    }

    /**
     * Create post with optional media URL
     */
    private void createPostWithMedia(String title, String content, String imageUrl) {
        // Build media list
        List<MediaItem> medias = null;
        if (imageUrl != null && !imageUrl.isEmpty()) {
            medias = new ArrayList<>();
            // MediaItem constructor: (String url, Integer width, Integer height)
            // Using null for width/height as we don't have image dimensions yet
            MediaItem mediaItem = new MediaItem(imageUrl, null, null);
            mediaItem.setSortOrder(0);
            medias.add(mediaItem);
        }

        // Create post request
        PostCreateRequest createRequest = new PostCreateRequest(
            title,
            content,
            true, // publish
            new ArrayList<>(createVm.getSelectedTags().getValue()),
            medias
        );

        // Call backend API
        PostRepository.getInstance().createPost(createRequest, new PostRepository.ResultCallback<Map<String, Long>>() {
            @Override
            public void onSuccess(Map<String, Long> result) {
                requireActivity().runOnUiThread(() -> {
                    // Clear draft and form
                    clearDraft();
                    clearForm();

                    Toast.makeText(requireContext(), "Published successfully!", Toast.LENGTH_SHORT).show();

                    // Navigate back to home
                    Intent intent = new Intent(requireContext(), MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    requireActivity().finish();
                });
            }

            @Override
            public void onError(String message) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Failed to publish: " + message, Toast.LENGTH_SHORT).show();
                    resetPublishButton();
                });
            }
        });
    }

    /**
     * Reset publish button to enabled state
     */
    private void resetPublishButton() {
        btnPublish.setEnabled(true);
        btnPublish.setText("Publish");
    }

    private void clearDraft() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREF_DRAFT, Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
    }

    private void clearForm() {
        // 使用ViewModel清空所有状态，UI会通过观察者自动更新
        createVm.clearAll();
        cameraImageUri = null;
        // 重置图片预览状态
        updateImagePreviewState();
        isDraftLoaded = false;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (glideRequestManager != null) {
            glideRequestManager.clear(imgPreview);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executor != null && !executor.isShutdown()) {
        executor.shutdown();
        }
    }
}
