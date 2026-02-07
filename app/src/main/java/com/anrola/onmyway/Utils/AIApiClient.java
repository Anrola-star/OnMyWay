package com.anrola.onmyway.Utils;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.anrola.onmyway.BuildConfig;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * AI接口调用工具类
 */
public class AIApiClient {
    private static final String TAG = "AIApiClient";
    // 接口基础配置
    private static final String BASE_URL = "https://ms-ens-3ff6312f-a684.api-inference.modelscope.cn/v1/chat/completions";
    private static final String API_KEY = BuildConfig.AI_API_KEY;
    private static final String MODEL_ID = "Qwen/Qwen3-4B";

    // OkHttp客户端
    private final OkHttpClient okHttpClient;
    // 主线程Handler（用于更新UI）
    private final Handler mainHandler;

    // 回调接口：用于返回流式结果和状态
    public interface AiCallback {
        // 接收流式返回的单段内容
        void onMessageReceived(String content);
        // 请求成功完成
        void onComplete();
        // 请求失败
        void onError(String errorMsg);
    }

    // 单例模式
    private static AIApiClient instance;
    public static AIApiClient getInstance() {
        if (instance == null) {
            instance = new AIApiClient();
        }
        return instance;
    }

    private AIApiClient() {
        // 初始化OkHttp客户端，设置超时时间
        okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        // 初始化主线程Handler
        mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * 调用AI接口（流式返回）
     * @param userPrompt 用户输入的提示词（包含JSON数据）
     * @param callback 结果回调
     */
    public void callAiApi(String userPrompt, String systemPrompt,AiCallback callback) {
        // 1. 构建请求体JSON
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("model", MODEL_ID);
            requestBody.put("stream", true); // 开启流式返回

            // 构建messages数组
            JSONArray messages = new JSONArray();
            // system角色：设置AI的行为
            JSONObject systemMsg = new JSONObject();
            systemMsg.put("role", "system");
            systemMsg.put("content", systemPrompt);
            messages.put(systemMsg);

            // user角色：用户输入的内容
            JSONObject userMsg = new JSONObject();
            userMsg.put("role", "user");
            userMsg.put("content", userPrompt);
            messages.put(userMsg);

            requestBody.put("messages", messages);
        } catch (Exception e) {
            callback.onError("构建请求参数失败：" + e.getMessage());
            return;
        }

        // 2. 构建请求
        Log.d(TAG, "构建请求...");
        Request request = new Request.Builder()
                .url(BASE_URL)
                .addHeader("Authorization", "Bearer " + API_KEY) // 认证头
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(
                        requestBody.toString(),
                        MediaType.parse("application/json; charset=utf-8")
                ))
                .build();

        // 3. 异步执行请求（流式读取）
        Log.d(TAG, "执行请求...");
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                // 网络请求失败，切换到主线程回调
                mainHandler.post(() -> callback.onError("网络请求失败：" + e.getMessage()));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    mainHandler.post(() -> callback.onError("接口返回错误：" + response.code()));
                    return;
                }
                Log.d(TAG,  "接口返回成功：" + response.code());
                // 流式读取响应体
                try (ResponseBody responseBody = response.body()) {
                    if (responseBody == null) {
                        mainHandler.post(() -> callback.onError("响应体为空"));
                        return;
                    }

                    // 逐行读取流式数据
                    for (String line : responseBody.source().readUtf8().split("\n")) {
                        line = line.trim();
                        // 过滤空行和结束标记
                        if (line.isEmpty() || line.equals("data: [DONE]")) {
                            continue;
                        }

                        // 解析流式数据（格式：data: { ... }）
                        if (line.startsWith("data: ")) {
                            String jsonStr = line.substring(6); // 去掉前缀"data: "
                            try {
                                JSONObject json = new JSONObject(jsonStr);
                                JSONArray choices = json.getJSONArray("choices");
                                if (choices.length() > 0) {
                                    JSONObject choice = choices.getJSONObject(0);
                                    JSONObject delta = choice.getJSONObject("delta");
                                    // 获取返回的内容（非空才回调）
                                    if (delta.has("content")) {
                                        String content = delta.getString("content");
                                        // 切换到主线程更新UI
                                        mainHandler.post(() -> callback.onMessageReceived(content));
                                    }
                                }
                            } catch (Exception e) {
                                // 单个数据解析失败，不中断整体流程
                                mainHandler.post(() -> callback.onError("解析数据失败：" + e.getMessage()));
                            }
                        }
                    }

                    // 所有数据读取完成
                    mainHandler.post(callback::onComplete);
                } catch (Exception e) {
                    mainHandler.post(() -> callback.onError("读取流式数据失败：" + e.getMessage()));
                } finally {
                    response.close();
                }
            }
        });
    }
}