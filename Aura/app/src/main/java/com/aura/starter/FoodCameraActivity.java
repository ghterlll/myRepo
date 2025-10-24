package com.aura.starter;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class FoodCameraActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA_PERMISSION = 100;
    private static final int REQUEST_STORAGE_PERMISSION = 101;
    private static final int REQUEST_IMAGE_CAPTURE = 102;
    private static final int REQUEST_IMAGE_PICK = 103;

    private PreviewView previewView;
    private ImageCapture imageCapture;
    private Camera camera;
    private boolean isCapturing = false;
    private int remainingShots = 3; // Daily limit

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_food_camera);

        initViews();
        setupClickListeners();
        checkPermissions();
    }

    private void initViews() {
        previewView = findViewById(R.id.previewView);
        
        // Update remaining shots display
        TextView tvRemaining = findViewById(R.id.tvRemaining);
        tvRemaining.setText("Remaining " + remainingShots + " times today");
    }

    private void setupClickListeners() {
        // Back button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        
        // Example button
        findViewById(R.id.btnExample).setOnClickListener(v -> {
            // TODO: Show example photos
            Toast.makeText(this, "Example feature coming soon", Toast.LENGTH_SHORT).show();
        });
        
        // Camera capture button
        findViewById(R.id.btnCapture).setOnClickListener(v -> {
            if (remainingShots > 0) {
                captureImage();
            } else {
                Toast.makeText(this, "Daily photo limit reached", Toast.LENGTH_SHORT).show();
            }
        });
        
        // Album button
        findViewById(R.id.btnAlbum).setOnClickListener(v -> openAlbum());
        
        // Mode selection button
        findViewById(R.id.btnFoodMode).setOnClickListener(v -> selectMode(true));
    }

    private void selectMode(boolean isFoodMode) {
        // Only food mode is available now
        TextView btnFood = findViewById(R.id.btnFoodMode);
        btnFood.setTextColor(ContextCompat.getColor(this, R.color.auragreen_primary));
        btnFood.setTextSize(16);
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        } else {
            startCamera();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "Camera permission required to take photos", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());
                
                imageCapture = new ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build();
                
                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                
                camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
                
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
                Toast.makeText(this, "Failed to start camera", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void captureImage() {
        if (imageCapture == null || isCapturing) return;
        
        isCapturing = true;
        
        // Create output file
        File photoFile = createImageFile();
        if (photoFile == null) {
            isCapturing = false;
            return;
        }
        
        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();
        
        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this), new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults output) {
                isCapturing = false;
                remainingShots--;
                updateRemainingDisplay();
                
                // Navigate to food recognition result
                Intent intent = new Intent(FoodCameraActivity.this, FoodRecognitionActivity.class);
                intent.putExtra("image_path", photoFile.getAbsolutePath());
                startActivity(intent);
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                isCapturing = false;
                Toast.makeText(FoodCameraActivity.this, "Photo capture failed: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openAlbum() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_STORAGE_PERMISSION);
            return;
        }
        
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_IMAGE_PICK);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_IMAGE_PICK && resultCode == RESULT_OK && data != null) {
            Uri selectedImage = data.getData();
            if (selectedImage != null) {
                // Navigate to food recognition result with selected image
                Intent intent = new Intent(this, FoodRecognitionActivity.class);
                intent.putExtra("image_uri", selectedImage.toString());
                startActivity(intent);
            }
        }
    }

    private File createImageFile() {
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String imageFileName = "FOOD_" + timeStamp;
            File storageDir = getExternalFilesDir(null);
            return File.createTempFile(imageFileName, ".jpg", storageDir);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void updateRemainingDisplay() {
        TextView tvRemaining = findViewById(R.id.tvRemaining);
        tvRemaining.setText("Remaining " + remainingShots + " times today");
    }
}
