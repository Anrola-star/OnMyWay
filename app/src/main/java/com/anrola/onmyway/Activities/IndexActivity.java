package com.anrola.onmyway.Activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.amap.api.services.core.LatLonPoint;
import com.anrola.onmyway.Entity.Order;
import com.anrola.onmyway.Fragments.NavFragment;
import com.anrola.onmyway.Fragments.OrderFragment;
import com.anrola.onmyway.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.anrola.onmyway.Utils.DeliveryRouteDynamic;
import com.anrola.onmyway.Utils.MyRequest;
import com.anrola.onmyway.Utils.SharedPreferencesManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
        //test(this);
    }

    // 测试函数
    public void test(Context context) {
    }
}