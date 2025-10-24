package com.aura.starter;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.aura.starter.network.AuraRepository;
import com.aura.starter.network.models.ApiResponse;
import com.aura.starter.network.models.FileUploadResponse;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FoodRecognitionActivity extends AppCompatActivity {

    private static final String TAG = "FoodRecognitionActivity";
    
    private ImageView imageView;
    private TextView tvStatus;
    private TextView tvCalories;
    private TextView tvFoodName;
    
    private AuraRepository repository;
    private ExecutorService executor;
    private Handler mainHandler;
    private File imageFile;
    private String uploadedImageUrl;
    private String uploadedImageKey;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_food_recognition);

        // Initialize components
        repository = new AuraRepository(this);
        executor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        // Check if user is logged in
        if (!isUserLoggedIn()) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            android.content.Intent intent = new android.content.Intent(this, LoginActivity.class);
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        initViews();
        loadImage();
        
        // Start real food recognition process
        startFoodRecognition();
    }
    
    private boolean isUserLoggedIn() {
        // Check if user has valid token using AuthManager
        return repository.getAuthManager().getAccessToken() != null;
    }

    private void initViews() {
        imageView = findViewById(R.id.imageView);
        tvStatus = findViewById(R.id.tvStatus);
        tvCalories = findViewById(R.id.tvCalories);
        tvFoodName = findViewById(R.id.tvFoodName);

        // Back button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        
        // Save button
        findViewById(R.id.btnSave).setOnClickListener(v -> saveFoodRecord());
        
        // Retake button
        findViewById(R.id.btnRetake).setOnClickListener(v -> finish());
    }

    private void loadImage() {
        String imagePath = getIntent().getStringExtra("image_path");
        String imageUri = getIntent().getStringExtra("image_uri");
        
        Bitmap bitmap = null;
        
        if (imagePath != null) {
            // Load from captured image
            imageFile = new File(imagePath);
            if (imageFile.exists()) {
                bitmap = BitmapFactory.decodeFile(imagePath);
            }
        } else if (imageUri != null) {
            // Load from selected image
            try {
                bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(Uri.parse(imageUri)));
                // For selected images, we need to create a temporary file
                imageFile = createTempFileFromUri(Uri.parse(imageUri));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
        } else {
            Toast.makeText(this, "Unable to load image", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void startFoodRecognition() {
        if (imageFile == null) {
            Toast.makeText(this, "No image to process", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Start the recognition process in background thread
        executor.execute(() -> {
            try {
                // Step 1: Upload image to OSS
                mainHandler.post(() -> tvStatus.setText("Uploading image..."));
                uploadImageToOSS();
                
                // Step 2: Simulate food recognition (replace with actual AI service)
                mainHandler.post(() -> tvStatus.setText("Recognizing food..."));
                Thread.sleep(2000); // Simulate processing time
                
                // Step 3: Show recognition results
                mainHandler.post(() -> {
                    tvStatus.setText("Recognition completed");
                    tvFoodName.setText("Apple"); // Replace with actual recognition result
                    tvCalories.setText("52 calories/100g");
                });
                
            } catch (Exception e) {
                Log.e(TAG, "Food recognition failed", e);
                mainHandler.post(() -> {
                    tvStatus.setText("Recognition failed");
                    Toast.makeText(this, "Food recognition failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }
    
    private void uploadImageToOSS() throws IOException {
        Log.d(TAG, "Uploading image to OSS: " + imageFile.getAbsolutePath());
        
        ApiResponse<FileUploadResponse> response = repository.uploadFoodImage(imageFile);
        
        if (response != null && response.isSuccess() && response.getData() != null) {
            uploadedImageUrl = response.getData().getUrl();
            uploadedImageKey = response.getData().getKey();
            Log.d(TAG, "Image uploaded successfully. URL: " + uploadedImageUrl + ", Key: " + uploadedImageKey);
        } else {
            // Handle token expiration
            if (response != null && response.getCode() == 1101) {
                Log.e(TAG, "Token expired, redirecting to login");
                mainHandler.post(() -> {
                    Toast.makeText(this, "Login expired, please login again", Toast.LENGTH_LONG).show();
                    // Redirect to login
                    android.content.Intent intent = new android.content.Intent(this, LoginActivity.class);
                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                });
                throw new IOException("Token expired");
            }
            
            String errorMsg = response != null ? response.getMessage() : "Unknown error";
            throw new IOException("Failed to upload image: " + errorMsg);
        }
    }
    
    private File createTempFileFromUri(Uri uri) {
        try {
            // Create a temporary file
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                    .format(new java.util.Date());
            String imageFileName = "FOOD_TEMP_" + timeStamp;
            File tempFile = File.createTempFile(imageFileName, ".jpg", getExternalFilesDir(null));
            
            // Copy content from URI to temp file
            java.io.InputStream inputStream = getContentResolver().openInputStream(uri);
            java.io.FileOutputStream outputStream = new java.io.FileOutputStream(tempFile);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            inputStream.close();
            outputStream.close();
            
            return tempFile;
        } catch (Exception e) {
            Log.e(TAG, "Failed to create temp file from URI", e);
            return null;
        }
    }

    private void saveFoodRecord() {
        if (uploadedImageKey == null) {
            Toast.makeText(this, "Image not uploaded yet", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // TODO: Save food record to database with uploaded image key
        // This would typically involve calling another API to save the food item
        // with the image key and recognition results
        
        Toast.makeText(this, "Food record saved with image", Toast.LENGTH_SHORT).show();
        finish();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null) {
            executor.shutdown();
        }
    }
}
