package com.example.onmyway.Activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.onmyway.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.example.onmyway.Utils.MyRequest;

public class IndexActivity extends AppCompatActivity {

    public static final String TAG = "MYLOG_IndexActivity";
    public Handler handler;

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
        Intent LoginIntent = new Intent(this, LoginActivity.class);
        startActivity(LoginIntent);

        //测试入口
        //Intent MainIntent = new Intent(this, MainActivity.class);
        //startActivity(MainIntent);
        //test();
    }

    // 测试函数
    private void test(){
        // 获取用户信息
        MyRequest myRequest = new MyRequest();
        handler = new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

                if (msg.what == 1) {
                    // 解析JSON
                    JSONObject response = null;
                    try {
                        response = new JSONObject((String) msg.obj);
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                    Log.d(TAG, response.toString());
                }else if (msg.what == 0) {
                    // 解析JSON
                    JSONArray response = null;
                    try {
                        response = new JSONArray((String) msg.obj);
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                    Log.d(TAG, response.toString());
                }



            }
        };


        myRequest.get("http://100.2.37.178:8080/user/all",handler,0);

        JSONObject json = new JSONObject();
        try {
            json.put("name","test");
            json.put("nickname","test");
            json.put("email","123");
            json.put("password","123");
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        myRequest.post("http://100.2.37.178:8080/user/register",json,handler,1);
    }
}