package com.anrola.onmyway.Utils;

import static java.lang.Math.max;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;

import java.util.Base64;

import android.util.Log;

import androidx.annotation.NonNull;

import com.alibaba.dashscope.aigc.multimodalconversation.AudioParameters;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import com.anrola.onmyway.BuildConfig;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Flowable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
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
 * 包含：1. 聊天对话（流式） 2. 语音合成 两类接口，支持自由切换使用
 */
public class AIApiClient {
    private static final String TAG = "AIApiClient";


    // ====================== 莫塔社区聊天接口配置 ======================
    //private static final String CHAT_BASE_URL = "https://ms-ens-3ff6312f-a684.api-inference.modelscope.cn/v1/chat/completions";
    private static final String CHAT_BASE_URL = "https://api-inference.modelscope.cn/v1/chat/completions";
    private static final String API_KEY_CHAT = BuildConfig.AI_API_KEY;
    private static final String CHAT_MODEL_ID = "Qwen/Qwen3-8B";

    // ====================== 阿里云百炼语音合成配置 ======================
    private static final String DASHSCOPE_MODEL = "qwen3-tts-flash"; // 语音合成模型
    private static final String DASHSCOPE_API_KEY = BuildConfig.DASHSCOPE_API_KEY; // 你的百炼API Key
    private static final String DASHSCOPE_REGION_URL = "https://dashscope.aliyuncs.com/api/v1"; // 北京地域（新加坡用：https://dashscope-intl.aliyuncs.com/api/v1）

    private final OkHttpClient okHttpClient;
    private final Handler mainHandler;


    // ====================== 回调接口定义 ======================

    /**
     * 聊天对话回调（流式）
     */
    public interface AiChatCallback {
        // 接收流式返回的单段内容
        void onMessageReceived(String content);

        // 请求成功完成
        void onComplete();

        // 请求失败
        void onError(String errorMsg);
    }

    /**
     * 语音合成回调（支持字节流/文件流返回）
     */
    public interface AiTtsCallback {
        // 接收语音合成的字节数据（流式/完整）
        void onAudioDataReceived(MultiModalConversationResult result);

        // 请求成功完成
        void onComplete(MultiModalConversationResult result);

        // 请求失败
        void onError(String errorMsg);
    }

    // ====================== 单例模式 ======================
    private static AIApiClient instance;

    public static AIApiClient getInstance() {
        if (instance == null) {
            instance = new AIApiClient();
        }
        return instance;
    }

    private AIApiClient() {
        // 初始化OkHttp客户端，设置超时时间（语音合成建议适当延长）
        okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS) // 语音合成读取超时延长
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        // 初始化主线程Handler
        mainHandler = new Handler(Looper.getMainLooper());
    }

    // ====================== 聊天对话接口 ======================

    /**
     * 调用AI聊天接口（流式返回）
     *
     * @param userPrompt   用户输入的提示词
     * @param systemPrompt 系统提示词（设置AI行为）
     * @param callback     结果回调
     */
    public void callChatApi(String userPrompt, String systemPrompt, AiChatCallback callback) {
        // 1. 构建请求体JSON
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("model", CHAT_MODEL_ID);
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
            callback.onError("构建聊天请求参数失败：" + e.getMessage());
            return;
        }

        // 构建请求
        Log.d(TAG, "构建聊天请求...");
        Request request = new Request.Builder()
                .url(CHAT_BASE_URL)
                .addHeader("Authorization", "Bearer " + API_KEY_CHAT) // 认证头
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(
                        requestBody.toString(),
                        MediaType.parse("application/json; charset=utf-8")
                ))
                .build();

        // 异步执行请求（流式读取）
        Log.d(TAG, "执行聊天请求...");
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                // 网络请求失败，切换到主线程回调
                mainHandler.post(() -> callback.onError("聊天网络请求失败：" + e.getMessage()));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    mainHandler.post(() -> callback.onError("聊天接口返回错误：" + response.code()));
                    return;
                }
                Log.d(TAG, "聊天接口返回成功：" + response.code());
                // 流式读取响应体
                try (ResponseBody responseBody = response.body()) {
                    if (responseBody == null) {
                        mainHandler.post(() -> callback.onError("聊天响应体为空"));
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
                                mainHandler.post(() -> callback.onError("聊天数据解析失败：" + e.getMessage()));
                            }
                        }
                    }

                    // 所有数据读取完成
                    mainHandler.post(callback::onComplete);
                } catch (Exception e) {
                    mainHandler.post(() -> callback.onError("读取聊天流式数据失败：" + e.getMessage()));
                } finally {
                    response.close();
                }
            }
        });
    }

    // ====================== 阿里云百炼语音合成接口 ======================

    /**
     * 阿里云百炼语音合成（基础版：默认参数）
     *
     * @param text     合成文本（支持中英）
     * @param callback 结果回调
     */
    public void callTtsApi(String text, boolean play,AiTtsCallback callback) {
        // 默认参数：Cherry音色、英文（可根据需求调整）
        callTtsApi(text, AudioParameters.Voice.CHERRY, "Chinese", play,callback);
    }

    /**
     * 阿里云百炼语音合成（自定义参数）
     *
     * @param text         合成文本
     * @param voice        音色（如CHERRY、LUNA、ERIC等）
     * @param languageType 语言（English/Chinese）
     * @param callback     结果回调
     */
    public void callTtsApi(String text, AudioParameters.Voice voice, String languageType, boolean play,AiTtsCallback callback) {

        // 子线程执行SDK调用，避免阻塞主线程
        new Thread(() -> {
            try {
                // 构建百炼TTS参数
                MultiModalConversationParam param = MultiModalConversationParam.builder()
                        .model(DASHSCOPE_MODEL)
                        .apiKey(DASHSCOPE_API_KEY)
                        .text(text) // 合成文本
                        .voice(voice) // 音色
                        .languageType(languageType) // 语言类型
                        .build();

                // 初始化百炼客户端并流式调用
                MultiModalConversation conv = new MultiModalConversation();
                Flowable<MultiModalConversationResult> resultFlow = conv.streamCall(param);
                resultFlow.blockingForEach(result -> {
                    String audioBase64 = result.getOutput().getAudio().getData();
                    byte[] audioBytes = Base64.getDecoder().decode(audioBase64);

                    callback.onAudioDataReceived(result);
                    if (play) {
                        AudioPlayer.playAudio(audioBytes);
                    }

                    if (result.getOutput().getAudio().getUrl() != null) {
                        callback.onComplete(result);
                        Log.d(TAG, "百炼语音合成完成: " + result.getOutput().getAudio().getUrl());
                    }
                });
            } catch (ApiException | NoApiKeyException | UploadFileException e) {
                String errorMsg = "百炼TTS调用异常：" + e.getMessage();
                Log.e(TAG, errorMsg, e);
                mainHandler.post(() -> callback.onError(errorMsg));
            }
        }).start();
    }


}