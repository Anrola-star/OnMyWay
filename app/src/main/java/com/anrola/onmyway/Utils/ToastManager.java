package com.anrola.onmyway.Utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.anrola.onmyway.R;

/**
 * 全局 Toast 管理类
 */
public class ToastManager {
    // 全局唯一 Toast 实例
    private static Toast sToast;
    // 主线程 Handler（处理子线程调用）
    private static final Handler sMainHandler = new Handler(Looper.getMainLooper());
    // 默认 Toast 布局
    private static View sToastView;

    /**
     * 私有化构造方法，禁止实例化
     */
    private ToastManager() {}

    /**
     * 显示短时长 Toast
     * @param context 上下文（传 Application Context）
     * @param message 提示文本
     */
    public static void showShortToast(@NonNull Context context, @NonNull String message) {
        showToast(context, message, Toast.LENGTH_SHORT);
    }

    /**
     * 显示长时长 Toast
     * @param context 上下文
     * @param message 提示文本
     */
    public static void showLongToast(@NonNull Context context, @NonNull String message) {
        showToast(context, message, Toast.LENGTH_LONG);
    }

    /**
     * 自定义位置的 Toast
     * @param context 上下文
     * @param message 提示文本
     * @param duration 时长
     */
    public static void showToast(
            @NonNull Context context,
            @NonNull String message,
            int duration
    ) {
        // 空文本返回
        if (message.isEmpty()) {
            return;
        }
        // 切换到主线程执行
        sMainHandler.post(() -> {
            Context appContext = context.getApplicationContext();
            // 取消已有 Toast（实现立即替换）
            cancelToast();

            // 初始化 Toast
            sToast = new Toast(appContext);
            sToast.setText(message);
            // 设置时长、位置
            sToast.setDuration(duration);
            // 显示 Toast
            sToast.show();
        });
    }

    /**
     * 显示系统默认样式 Toast（无自定义布局）
     * @param context 上下文
     * @param message 提示文本
     * @param duration 时长
     */
    public static void showSystemToast(@NonNull Context context, @NonNull String message, int duration) {
        if (message.isEmpty()) {
            return;
        }
        sMainHandler.post(() -> {
            Context appContext = context.getApplicationContext();
            cancelToast();
            sToast = Toast.makeText(appContext, message, duration);
            sToast.setGravity(Gravity.CENTER, 0, 0);
            sToast.show();
        });
    }

    /**
     * 手动取消 Toast
     */
    public static void cancelToast() {
        if (sToast != null) {
            sToast.cancel();
            sToast = null;
        }
    }

    /**
     * 清空 Toast 缓存（如退出应用时调用）
     */
    public static void clearCache() {
        cancelToast();
        sToastView = null;
        sMainHandler.removeCallbacksAndMessages(null);
    }
}