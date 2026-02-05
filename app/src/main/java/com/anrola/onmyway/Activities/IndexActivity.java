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

    //证书指纹R:
    //SHA1: 10:E7:96:B9:41:24:7D:BB:05:64:02:B0:7D:90:E1:BB:7D:2C:42:26
    //SHA256: 10:17:06:65:BB:A3:0D:EF:C8:29:F2:48:B0:00:63:F3:0D:29:77:FE:E2:F5:2F:94:78:0A:8C:7F:DB:F7:7C:AD


    //证书指纹D:
    //SHA1: 61:F4:CB:01:03:CD:B1:1B:C5:A8:8D:2A:04:63:0C:10:1B:D4:E1:8F
    //SHA256: 12:5C:3C:0F:1B:32:0A:EB:95:79:55:88:84:E0:25:A7:C4:25:D3:47:44:03:C9:74:F0:C9:FD:13:8B:8F:FF:27

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