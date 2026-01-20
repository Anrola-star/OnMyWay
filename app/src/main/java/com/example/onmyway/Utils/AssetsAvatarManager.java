package com.example.onmyway.Utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class AssetsAvatarManager {
    private static final String TAG = "AssetsAvatarManager";
    private static final String AVATAR_ASSETS_DIR = "avatars";
    private final Context context;

    public AssetsAvatarManager(Context context) {
        // 避免内存泄漏，使用Application Context
        this.context = context.getApplicationContext();
    }

    /**
     * 读取assets中的头像
     * @param avatarIndex 头像索引（0-99）
     * @return 头像Bitmap，失败返回null
     */
    public Bitmap getAvatar(int avatarIndex) {
        InputStream inputStream = null;
        try {
            // 拼接assets文件路径
            String fileName = "avatars/avatar_" + avatarIndex + ".png";
            inputStream = context.getAssets().open(fileName);

            // 解码为Bitmap
            return BitmapFactory.decodeStream(inputStream);
        } catch (IOException e) {
            Log.e(TAG, "读取头像失败：index=" + avatarIndex, e);
            return null;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 批量获取所有头像（按需使用）
     * @return 头像数组
     */
    public List<Bitmap> getAllAvatars() {
        List<Bitmap> avatars = new ArrayList<>();
        for (int i = 0; i < getAvatarCount(); i++) {
            avatars.add(getAvatar(i));
        }
        return avatars;
    }

    /**
     * 获取assets中的头像数量
     * @return 头像数量
     */
    public int getAvatarCount() {
        try {
            // 列出assets/avatars下的所有文件
            String[] fileNames = context.getAssets().list(AVATAR_ASSETS_DIR);
            if (fileNames == null || fileNames.length == 0) {
                return 0;
            }

            // 过滤出符合格式的头像文件
            int count = 0;
            for (String fileName : fileNames) {
                count++;
            }
            return count;
        } catch (IOException e) {
            Log.e(TAG, "统计assets头像数量失败", e);
            return 0;
        }
    }
}
