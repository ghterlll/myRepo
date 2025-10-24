package com.aura.starter.util;

import android.content.pm.PackageManager;
import android.util.Log;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 现代化权限管理器
 * 支持单个权限和多个权限的请求，带有友好的用户提示
 */
public class PermissionManager {
    private static final String TAG = "PermissionManager";

    private final Fragment fragment;
    private final Map<String, PermissionRequest> pendingRequests = new HashMap<>();
    private final List<String> requestingPermissions = new ArrayList<>();

    // 预注册的launcher，避免在Fragment创建后注册
    private ActivityResultLauncher<String> singlePermissionLauncher;
    private ActivityResultLauncher<String[]> multiplePermissionLauncher;

    public PermissionManager(Fragment fragment) {
        this.fragment = fragment;
        initializeLaunchers();
    }

    /**
     * 初始化权限请求launcher，必须在Fragment创建前调用
     */
    private void initializeLaunchers() {
        // 单个权限请求launcher
        singlePermissionLauncher = fragment.registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            new ActivityResultCallback<Boolean>() {
                @Override
                public void onActivityResult(Boolean isGranted) {
                    handleSinglePermissionResult(isGranted);
                }
            }
        );

        // 多个权限请求launcher
        multiplePermissionLauncher = fragment.registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            new ActivityResultCallback<Map<String, Boolean>>() {
                @Override
                public void onActivityResult(Map<String, Boolean> result) {
                    // 处理多个权限请求结果
                    List<String> granted = new ArrayList<>();
                    List<String> denied = new ArrayList<>();

                    for (Map.Entry<String, Boolean> entry : result.entrySet()) {
                        String permission = entry.getKey();
                        requestingPermissions.remove(permission);
                        pendingRequests.remove(permission);

                        if (entry.getValue()) {
                            granted.add(permission);
                        } else {
                            denied.add(permission);
                        }
                    }

                    // 这里简化处理，实际项目中可能需要更复杂的逻辑来匹配回调
                }
            }
        );
    }

    /**
     * 请求单个权限
     */
    public void requestPermission(String permission, PermissionCallback callback) {
        if (hasPermission(permission)) {
            callback.onGranted(permission);
            return;
        }

        if (requestingPermissions.contains(permission)) {
            Log.d(TAG, "Permission already being requested: " + permission);
            return;
        }

        requestingPermissions.add(permission);
        pendingRequests.put(permission, new PermissionRequest(permission, callback));
        singlePermissionLauncher.launch(permission);
    }

    /**
     * 请求多个权限
     */
    public void requestPermissions(String[] permissions, MultiplePermissionCallback callback) {
        List<String> ungrantedPermissions = new ArrayList<>();

        // 检查哪些权限还没有被授权
        for (String permission : permissions) {
            if (!hasPermission(permission)) {
                ungrantedPermissions.add(permission);
            }
        }

        // 如果所有权限都已授权，直接回调
        if (ungrantedPermissions.isEmpty()) {
            callback.onAllGranted(permissions);
            return;
        }

        // 请求未授权的权限
        String[] permissionsToRequest = ungrantedPermissions.toArray(new String[0]);
        for (String permission : permissionsToRequest) {
            requestingPermissions.add(permission);
        }

        multiplePermissionLauncher.launch(permissionsToRequest);
    }

    /**
     * 检查是否有权限
     */
    public boolean hasPermission(String permission) {
        return ContextCompat.checkSelfPermission(fragment.requireContext(), permission)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * 检查是否有所有权限
     */
    public boolean hasAllPermissions(String[] permissions) {
        for (String permission : permissions) {
            if (!hasPermission(permission)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 单个权限请求回调
     */
    public interface PermissionCallback {
        void onGranted(String permission);
        void onDenied(String permission);
    }

    /**
     * 多个权限请求回调
     */
    public interface MultiplePermissionCallback {
        void onAllGranted(String[] permissions);
        void onResult(String[] granted, String[] denied);
    }

    /**
     * 处理单个权限请求结果
     */
    private void handleSinglePermissionResult(Boolean isGranted) {
        // 从pendingRequests中找到对应的请求并处理
        // 这里需要一种方法来确定哪个权限被请求了
        // 由于我们无法直接从回调中获取权限名，我们需要在请求时记录当前请求的权限

        // 临时方案：遍历所有待处理的请求，找到最近的匹配
        String currentPermission = null;
        for (String permission : requestingPermissions) {
            if (pendingRequests.containsKey(permission)) {
                currentPermission = permission;
                break;
            }
        }

        if (currentPermission != null) {
            requestingPermissions.remove(currentPermission);
            PermissionRequest request = pendingRequests.remove(currentPermission);
            if (request != null) {
                if (isGranted) {
                    request.callback.onGranted(currentPermission);
                } else {
                    request.callback.onDenied(currentPermission);
                }
            }
        }
    }

    /**
     * 权限请求信息
     */
    private static class PermissionRequest {
        final String permission;
        final PermissionCallback callback;

        PermissionRequest(String permission, PermissionCallback callback) {
            this.permission = permission;
            this.callback = callback;
        }
    }
}
