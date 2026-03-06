package com.anrola.onmyway.Activities;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.anrola.onmyway.R;
import com.anrola.onmyway.Utils.EditTextController;
import com.anrola.onmyway.Utils.MyRequest;
import com.anrola.onmyway.Utils.SharedPreferencesManager;
import com.anrola.onmyway.Utils.ToastManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class UserInfoActivity extends AppCompatActivity {


    private Context context;
    private MyRequest myRequest;
    private Handler handler;
    private SharedPreferencesManager sharedPreferencesManager;

    private class Views {
        private TextView tvUserId;
        private EditText etNickName;
        private EditText etName;
        private EditText etPhone;
        private TextView tvPassword;
        private EditText etPassword;
        private EditText etNewPassword;
        private EditText etConfirmNewPassword;
        private EditText etCaptcha;
        private ImageView ivCaptcha;
        private Button btnChangePassword;
        private Button btnEdit;
        private Button btnConfirm;
        private Button btnCancel;
        private LinearLayout layoutNewPassword;
        private LinearLayout layoutConfirmNewPassword;
        private LinearLayout layoutCaptcha;
    }
    private Views views = new Views();


    private EditTextController editTextController;
    private final int getCaptchaHandlerWhat = 1;
    private final int getUserInfoHandlerWhat = 2;
    private final int updateUserInfoHandlerWhat = 3;
    private Map<String, String> sharedData = new HashMap<>();
    private final String sessionId_SharedDataKey = "sessionId";
    private final String id_SharedDataKey = "id";
    private final String name_SharedDataKey = "name";
    private final String nickname_SharedDataKey = "nickname";
    private final String phone_SharedDataKey = "phone";
    private final String password_SharedDataKey = "password";
    private final String TAG = "UserInfoActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_info);

        initView();
        initHandler();
        requestUserInfo();
        setClickEvents();

    }

    private void initView() {
        context = this;
        myRequest = new MyRequest();
        editTextController = new EditTextController();
        sharedPreferencesManager = SharedPreferencesManager.getInstance(context);
        views.tvUserId = findViewById(R.id.uia_tv_user_id);
        views.etNickName = findViewById(R.id.uia_et_nickname);
        views.etName = findViewById(R.id.uia_et_user_name);
        views.etPhone = findViewById(R.id.uia_et_phone);
        views.tvPassword = findViewById(R.id.uia_tv_password);
        views.etPassword = findViewById(R.id.uia_et_password);
        views.etNewPassword = findViewById(R.id.uia_et_new_password);
        views.etConfirmNewPassword = findViewById(R.id.uia_et_confirm_new_password);
        views.etCaptcha = findViewById(R.id.uia_et_captcha);
        views.ivCaptcha = findViewById(R.id.uia_iv_captcha);
        views.btnChangePassword = findViewById(R.id.uia_btn_change_password);
        views.btnEdit = findViewById(R.id.uia_btn_edit_info);
        views.btnConfirm = findViewById(R.id.uia_btn_confirm_edit);
        views.btnCancel = findViewById(R.id.uia_btn_cancel_edit);
        views.layoutNewPassword = findViewById(R.id.uia_lo_new_password);
        views.layoutConfirmNewPassword = findViewById(R.id.uia_lo_confirm_new_password);
        views.layoutCaptcha = findViewById(R.id.uia_lo_captcha);
    }

    private void setClickEvents() {
        views.btnChangePassword.setOnClickListener(v -> {
            switchChangePasswordMode(true);
            switchEditMode(true);
        });

        views.btnEdit.setOnClickListener(v -> {
            switchEditMode(true);
        });

        views.btnConfirm.setOnClickListener(v -> {
            switchEditMode(false);
        });

        views.btnCancel.setOnClickListener(v -> {


            resetUserInfo();
            switchEditMode(false);
            switchCaptchaMode(false);
            switchChangePasswordMode(false);
        });
    }

    private void setListeners() {
        views.etPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                views.layoutConfirmNewPassword.setVisibility(View.VISIBLE);
                views.layoutCaptcha.setVisibility(View.VISIBLE);
                switchCaptchaMode(true);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        views.etConfirmNewPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String p1 = views.etNewPassword.getText().toString();
                String p2 = views.etConfirmNewPassword.getText().toString();
                if (!p1.equals(p2)) {
                    editTextController.startBorderWidthBlinkAnimation(
                            UserInfoActivity.this, views.etConfirmNewPassword, getColor(R.color.red),
                            0, 3, 8, 600, 3);
                } else {
                    editTextController.startBorderWidthBlinkAnimation(
                            UserInfoActivity.this, views.etConfirmNewPassword, getColor(android.R.color.transparent),
                            0, 0, 8, 600, 0);
                }
            }
        });
        views.etPhone.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                switchCaptchaMode(true);
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
        views.etName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                switchCaptchaMode(true);
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void initHandler() {
        handler = new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);

                // 空数据处理
                if (msg.obj == null) {
                    Log.e(TAG, "JSON解析失败：响应字符串为空");
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setTitle("错误")
                            .setMessage("请求失败, 无响应")
                            .setPositiveButton("确定", null)
                            .show();
                    return; // 终止后续解析逻辑
                }

                // 响应数据处理
                JSONObject response = null;
                switch (msg.what){
                    case getCaptchaHandlerWhat: // 获取验证码, 设置验证码图片
                        try {
                            response = new JSONObject((String) msg.obj);
                            if (response.getInt("code") == 200) {
                                JSONObject data = response.getJSONObject("data");
                                sharedData.put(sessionId_SharedDataKey, data.getString("sessionId"));
                                String captchaBase64 = data.getString("imageBase64");
                                byte[] captchaBase64Bytes = Base64.decode(captchaBase64, Base64.DEFAULT);
                                views.ivCaptcha.setImageBitmap(BitmapFactory.decodeByteArray(captchaBase64Bytes, 0, captchaBase64Bytes.length));
                            } else if (response.getInt("code") == 500) {
                                ToastManager.showToast(context, "验证码获取错误", Toast.LENGTH_SHORT);
                            }
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                        break;
                    case getUserInfoHandlerWhat:    // 获取用户信息, 设置用户信息, 设置输入框监听
                        try {
                            response = new JSONObject((String) msg.obj);
                            if (response.getInt("code") == 200) {
                                JSONObject data = response.getJSONObject("data");
                                sharedData.put(id_SharedDataKey, data.getString("id"));
                                sharedData.put(name_SharedDataKey, data.getString("name"));
                                sharedData.put(nickname_SharedDataKey, data.getString("nickname"));
                                sharedData.put(phone_SharedDataKey, data.getString("phone"));

                                views.tvUserId.setText(sharedData.get("id"));
                                views.etName.setText(sharedData.get("name"));
                                views.etNickName.setText(sharedData.get("nickname"));
                                views.etPhone.setText(sharedData.get("phone"));

                                setListeners(); // 设置输入框监听
                            }else if (response.getInt("code") == 500) {
                                ToastManager.showToast(context, "用户信息获取错误", Toast.LENGTH_SHORT);
                            }
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                        break;
                        case updateUserInfoHandlerWhat:
                            try {
                                response = new JSONObject((String) msg.obj);
                                if (response.getInt("code") == 200) {
                                    ToastManager.showToast(context, "用户信息更新成功", Toast.LENGTH_SHORT);
                                    switchEditMode(false);
                                    JSONObject data = response.getJSONObject("data");
                                    sharedData.put(id_SharedDataKey, data.getString("id"));
                                    sharedData.put(name_SharedDataKey, data.getString("name"));
                                    sharedData.put(nickname_SharedDataKey, data.getString("nickname"));
                                    sharedData.put(phone_SharedDataKey, data.getString("phone"));
                                    views.tvUserId.setText(sharedData.get("id"));
                                    views.etName.setText(sharedData.get("name"));
                                    views.etNickName.setText(sharedData.get("nickname"));
                                    views.etPhone.setText(sharedData.get("phone"));
                                }
                            } catch (JSONException e) {
                                throw new RuntimeException(e);
                            }
                }
            }
        };
    }

    private void switchEditMode(Boolean isEdit) {
        if (isEdit) {
            setEditTextEnabled(views.etNickName, true);
            setEditTextEnabled(views.etName, true);
            setEditTextEnabled(views.etPhone, true);

            views.btnEdit.setVisibility(View.GONE);
            views.btnConfirm.setVisibility(View.VISIBLE);
            views.btnCancel.setVisibility(View.VISIBLE);
        } else {
            setEditTextEnabled(views.etNickName, false);
            setEditTextEnabled(views.etName, false);
            setEditTextEnabled(views.etPhone, false);

            views.btnEdit.setVisibility(View.VISIBLE);
            views.btnConfirm.setVisibility(View.GONE);
            views.btnCancel.setVisibility(View.GONE);
        }
    }

    private void requestCaptcha() {
        // 获取验证码
        String urlString = myRequest.getBaseURL(this) + "/captcha/get";
        myRequest.get(urlString, handler, getCaptchaHandlerWhat);
    }

    private void switchCaptchaMode(Boolean isShowCaptcha) {
        if (isShowCaptcha) {
            views.etCaptcha.setBackgroundResource(R.drawable.edit_text_bg);
            views.etCaptcha.setEnabled(true);
            views.layoutCaptcha.setVisibility(View.VISIBLE);
            if (!sharedData.containsKey(sessionId_SharedDataKey)) {
                requestCaptcha();
            }
        } else {
            views.layoutCaptcha.setVisibility(View.GONE);
            views.etCaptcha.setText("");
            views.etCaptcha.setEnabled(true);
            views.etCaptcha.setBackgroundResource(android.R.color.transparent);
        }
    }

    private void switchChangePasswordMode(Boolean isChangePassword) {
        if (isChangePassword) {
            views.tvPassword.setText("旧密码：");
            views.etPassword.setText("");

            setEditTextEnabled(views.etPassword, true);
            setEditTextEnabled(views.etNewPassword, true);
            setEditTextEnabled(views.etConfirmNewPassword, true);

            views.layoutNewPassword.setVisibility(View.VISIBLE);
            views.layoutConfirmNewPassword.setVisibility(View.VISIBLE);
        } else {

            setEditTextEnabled(views.etPassword, false);
            setEditTextEnabled(views.etNewPassword, false);
            setEditTextEnabled(views.etConfirmNewPassword, false);

            views.layoutNewPassword.setVisibility(View.GONE);
            views.layoutConfirmNewPassword.setVisibility(View.GONE);
        }
    }

    private void setEditTextEnabled(EditText editText, Boolean isEnabled) {
        if (isEnabled) {
            editText.setEnabled(true);
            editText.setBackgroundResource(R.drawable.edit_text_bg);
        } else {
            editText.setEnabled(false);
            editText.setBackgroundResource(android.R.color.transparent);
        }
    }

    private void requestUserInfo() {
        String urlString = myRequest.getBaseURL(this) + "/user/id";
        String key = getString(R.string.shared_preferences_token_key);
        String token = sharedPreferencesManager.get(key);
        myRequest.get(
                urlString,
                handler,
                getUserInfoHandlerWhat,
                token);
    }

    private void resetUserInfo() {
        views.tvUserId.setText(sharedData.get("id"));
        views.etName.setText(sharedData.get("name"));
        views.etNickName.setText(sharedData.get("nickname"));
        views.etPhone.setText(sharedData.get("phone"));
        views.tvPassword.setText("密码：");
        views.etPassword.setText("********");
        views.etNewPassword.setText("");
        views.etConfirmNewPassword.setText("");

    }
    private void updateUserInfo(String nickname, String phone, String password) {
        // 更新用户信息
        String urlString = myRequest.getBaseURL(this) + "/user/update";
        JSONObject json = new JSONObject();
        try {
            json.put("nickname", nickname);
            json.put("phone", phone);
            json.put("password", password);
        }catch (JSONException e){
            throw new RuntimeException(e);
        }
        myRequest.post(
                urlString,
                json,
                handler,
                updateUserInfoHandlerWhat
        );
    }

}