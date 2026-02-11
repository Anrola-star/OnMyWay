package com.anrola.onmyway.Activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.alibaba.dashscope.aigc.multimodalconversation.AudioParameters;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.amap.api.services.core.LatLonPoint;
import com.anrola.onmyway.Entity.Order;
import com.anrola.onmyway.Fragments.NavFragment;
import com.anrola.onmyway.Fragments.OrderFragment;
import com.anrola.onmyway.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.anrola.onmyway.Utils.AIApiClient;
import com.anrola.onmyway.Utils.AudioPlayer;
import com.anrola.onmyway.Utils.DeliveryRouteDynamic;
import com.anrola.onmyway.Utils.MyRequest;
import com.anrola.onmyway.Utils.SharedPreferencesManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.reactivex.Flowable;

public class IndexActivity extends AppCompatActivity {

    public static final String TAG = "MYLOG_IndexActivity";
    public final Context context = this;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_index);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        //入口
        //Intent LoginIntent = new Intent(this, LoginActivity.class);
        //startActivity(LoginIntent);


        //测试入口
        //Intent MainIntent = new Intent(this, MainActivity.class);
        //startActivity(MainIntent);
        test();
    }

    // 测试函数
    public void test() {
        //callChatAI();
        callTTSAI();
    }

    private void callChatAI(){
        String textToChat = "\"你好，你支持语音合成吗\",将此段生成为语音";
        AIApiClient aiApiClient = AIApiClient.getInstance();
        aiApiClient.callChatApi(
                textToChat,
                "你是一个助手",
                new AIApiClient.AiChatCallback() {

                    @Override
                    public void onMessageReceived(String content) {
                        Log.d("MYLOG_IndexActivity", "onMessageReceived: " + content);
                    }

                    @Override
                    public void onComplete() {
                    }

                    @Override
                    public void onError(String errorMsg) {

                    }
                }
        );
    }
    private void callTTSAI(){
        String text = "我见过龙";
        AIApiClient aiApiClient = AIApiClient.getInstance();
        aiApiClient.callTtsApi(
                text,
                AudioParameters.Voice.CHERRY,
                "Chinese",false,
                new AIApiClient.AiTtsCallback() {
                    @Override
                    public void onAudioDataReceived(MultiModalConversationResult result) {

                    }

                    @Override
                    public void onComplete(MultiModalConversationResult  result) {
                        String audioUrl = result.getOutput().getAudio().getUrl();
                        AudioPlayer.playAudioByUrl(audioUrl);
                    }

                    @Override
                    public void onError(String errorMsg) {
                    }
                }
        );
    }
}