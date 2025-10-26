package com.aura.starter.util;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;
import com.aura.starter.R;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Unified Glide configuration utility for consistent image loading across the app
 *
 * Features:
 * - Consistent placeholder and error images
 * - Automatic image source detection (URL, file, asset, resource)
 * - Unified caching strategy
 * - Proper error handling
 */
public class GlideUtils {

    /**
     * Default request options for all image loads
     */
    private static RequestOptions getDefaultOptions() {
        return new RequestOptions()
            .placeholder(R.drawable.placeholder)
            .error(R.drawable.placeholder)
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC);
    }

    /**
     * Load image with automatic source detection
     * Handles URLs, file paths, assets, and resource IDs
     */
    public static void loadImage(Context context, String imageUri, ImageView imageView) {
        if (imageUri == null || imageUri.isEmpty()) {
            imageView.setImageResource(R.drawable.placeholder);
            return;
        }

        // Priority 1: HTTP/HTTPS URLs (MinIO, web sources)
        if (imageUri.startsWith("http://") || imageUri.startsWith("https://")) {
            Glide.with(context)
                .load(imageUri)
                .apply(getDefaultOptions())
                .into(imageView);
        }
        // Priority 2: Local file paths
        else if (imageUri.startsWith("/") || imageUri.startsWith("file://")) {
            Glide.with(context)
                .load(new File(imageUri.replace("file://", "")))
                .apply(getDefaultOptions())
                .into(imageView);
        }
        // Priority 3: Assets images (imgX format)
        else if (imageUri.startsWith("img") && imageUri.length() <= 5) {
            loadAssetImage(context, imageUri, imageView);
        }
        // Priority 4: Resource IDs (numeric strings)
        else {
            try {
                int resourceId = Integer.parseInt(imageUri);
                Glide.with(context)
                    .load(resourceId)
                    .apply(getDefaultOptions())
                    .into(imageView);
            } catch (NumberFormatException e) {
                // Fallback: try as URL
                Glide.with(context)
                    .load(imageUri)
                    .apply(getDefaultOptions())
                    .into(imageView);
            }
        }
    }

    /**
     * Load image from assets folder
     */
    private static void loadAssetImage(Context context, String imageName, ImageView imageView) {
        try {
            AssetManager assetManager = context.getAssets();
            InputStream inputStream = assetManager.open("images/" + imageName + ".png");
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            inputStream.close();

            if (bitmap != null) {
                Glide.with(context)
                    .load(bitmap)
                    .apply(getDefaultOptions())
                    .into(imageView);
            } else {
                imageView.setImageResource(R.drawable.placeholder);
            }
        } catch (IOException e) {
            imageView.setImageResource(R.drawable.placeholder);
        }
    }

    /**
     * Load image with custom options (for special cases like fitCenter, centerCrop)
     */
    public static void loadImageWithOptions(Context context, String imageUri, ImageView imageView, RequestOptions customOptions) {
        if (imageUri == null || imageUri.isEmpty()) {
            imageView.setImageResource(R.drawable.placeholder);
            return;
        }

        RequestOptions options = customOptions
            .placeholder(R.drawable.placeholder)
            .error(R.drawable.placeholder);

        Glide.with(context)
            .load(imageUri)
            .apply(options)
            .into(imageView);
    }

    /**
     * Clear image from ImageView
     */
    public static void clear(Context context, ImageView imageView) {
        Glide.with(context).clear(imageView);
    }
}
