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
import androidx.recyclerview.widget.RecyclerView;

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

    private class ViewHolder{
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
    private ViewHolder viewHolder;


    private EditTextController editTextController;
    private final int getCaptchaHandlerWhat = 1;
    private final int getUserInfoHandlerWhat = 2;
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
        viewHolder.tvUserId = findViewById(R.id.uia_tv_user_id);
        viewHolder.etNickName = findViewById(R.id.uia_et_nickname);
        viewHolder.etName = findViewById(R.id.uia_et_user_name);
        viewHolder.etPhone = findViewById(R.id.uia_et_phone);
        viewHolder.tvPassword = findViewById(R.id.uia_tv_password);
        viewHolder.etPassword = findViewById(R.id.uia_et_password);
        viewHolder.etNewPassword = findViewById(R.id.uia_et_new_password);
        viewHolder.etConfirmNewPassword = findViewById(R.id.uia_et_confirm_new_password);
        viewHolder.etCaptcha = findViewById(R.id.uia_et_captcha);
        viewHolder.ivCaptcha = findViewById(R.id.uia_iv_captcha);
        viewHolder.btnChangePassword = findViewById(R.id.uia_btn_change_password);
        viewHolder.btnEdit = findViewById(R.id.uia_btn_edit_info);
        viewHolder.btnConfirm = findViewById(R.id.uia_btn_confirm_edit);
        viewHolder.btnCancel = findViewById(R.id.uia_btn_cancel_edit);
        viewHolder.layoutNewPassword = findViewById(R.id.uia_lo_new_password);
        viewHolder.layoutConfirmNewPassword = findViewById(R.id.uia_lo_confirm_new_password);
        viewHolder.layoutCaptcha = findViewById(R.id.uia_lo_captcha);
    }

    private void setClickEvents() {
        viewHolder.btnChangePassword.setOnClickListener(v -> {
            switchChangePasswordMode(true);
            switchEditMode(true);
        });

        viewHolder.btnEdit.setOnClickListener(v -> {
            switchEditMode(true);
        });

        viewHolder.btnConfirm.setOnClickListener(v -> {
            switchEditMode(false);
        });

        viewHolder.btnCancel.setOnClickListener(v -> {
            switchChangePasswordMode(false);
            switchEditMode(false);
            switchCaptchaMode(false);
            resetUserInfo();
        });
    }

    private void setListeners() {
        viewHolder.etPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewHolder.layoutConfirmNewPassword.setVisibility(View.VISIBLE);
                viewHolder.layoutCaptcha.setVisibility(View.VISIBLE);
                switchCaptchaMode(true);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        viewHolder.etConfirmNewPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String p1 = viewHolder.etNewPassword.getText().toString();
                String p2 = viewHolder.etConfirmNewPassword.getText().toString();
                if (!p1.equals(p2)) {
                    editTextController.startBorderWidthBlinkAnimation(
                            UserInfoActivity.this, viewHolder.etConfirmNewPassword, getColor(R.color.red),
                            0, 3, 8, 600, 3);
                } else {
                    editTextController.startBorderWidthBlinkAnimation(
                            UserInfoActivity.this, viewHolder.etConfirmNewPassword, getColor(android.R.color.transparent),
                            0, 0, 8, 600, 0);
                }
            }
        });
        viewHolder.etPhone.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                switchCaptchaMode(true);
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
        viewHolder.etName.addTextChangedListener(new TextWatcher() {
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
                                viewHolder.ivCaptcha.setImageBitmap(BitmapFactory.decodeByteArray(captchaBase64Bytes, 0, captchaBase64Bytes.length));
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

                                viewHolder.tvUserId.setText(sharedData.get("id"));
                                viewHolder.etName.setText(sharedData.get("name"));
                                viewHolder.etNickName.setText(sharedData.get("nickname"));
                                viewHolder.etPhone.setText(sharedData.get("phone"));

                                setListeners(); // 设置输入框监听
                            }else if (response.getInt("code") == 500) {
                                ToastManager.showToast(context, "用户信息获取错误", Toast.LENGTH_SHORT);
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
            setEditTextEnabled(viewHolder.etNickName, true);
            setEditTextEnabled(viewHolder.etName, true);
            setEditTextEnabled(viewHolder.etPhone, true);

            viewHolder.btnEdit.setVisibility(View.GONE);
            viewHolder.btnConfirm.setVisibility(View.VISIBLE);
            viewHolder.btnCancel.setVisibility(View.VISIBLE);
        } else {
            setEditTextEnabled(viewHolder.etNickName, false);
            setEditTextEnabled(viewHolder.etName, false);
            setEditTextEnabled(viewHolder.etPhone, false);

            viewHolder.btnEdit.setVisibility(View.VISIBLE);
            viewHolder.btnConfirm.setVisibility(View.GONE);
            viewHolder.btnCancel.setVisibility(View.GONE);
        }
    }

    private void requestCaptcha() {
        // TODO: 获取验证码
        String urlString = myRequest.getBaseURL(this) + "/captcha/get";
        myRequest.get(urlString, handler, getCaptchaHandlerWhat);
    }

    private void switchCaptchaMode(Boolean isShowCaptcha) {
        if (isShowCaptcha) {
            viewHolder.etCaptcha.setBackgroundResource(R.drawable.edit_text_bg);
            viewHolder.etCaptcha.setEnabled(true);
            viewHolder.layoutCaptcha.setVisibility(View.VISIBLE);
            if (!sharedData.containsKey(sessionId_SharedDataKey)) {
                requestCaptcha();
            }
        } else {
            viewHolder.layoutCaptcha.setVisibility(View.GONE);
            viewHolder.etCaptcha.setText("");
            viewHolder.etCaptcha.setEnabled(true);
            viewHolder.etCaptcha.setBackgroundResource(android.R.color.transparent);
        }
    }

    private void switchChangePasswordMode(Boolean isChangePassword) {
        if (isChangePassword) {
            viewHolder.tvPassword.setText("旧密码：");
            viewHolder.etPassword.setText("");

            setEditTextEnabled(viewHolder.etPassword, true);
            setEditTextEnabled(viewHolder.etNewPassword, true);
            setEditTextEnabled(viewHolder.etConfirmNewPassword, true);

            viewHolder.layoutNewPassword.setVisibility(View.VISIBLE);
            viewHolder.layoutConfirmNewPassword.setVisibility(View.VISIBLE);
        } else {

            setEditTextEnabled(viewHolder.etPassword, false);
            setEditTextEnabled(viewHolder.etNewPassword, false);
            setEditTextEnabled(viewHolder.etConfirmNewPassword, false);

            viewHolder.layoutNewPassword.setVisibility(View.GONE);
            viewHolder.layoutConfirmNewPassword.setVisibility(View.GONE);
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
        viewHolder.tvUserId.setText(sharedData.get("id"));
        viewHolder.etName.setText(sharedData.get("name"));
        viewHolder.etNickName.setText(sharedData.get("nickname"));
        viewHolder.etPhone.setText(sharedData.get("phone"));
        viewHolder.tvPassword.setText("密码：");
        viewHolder.etPassword.setText("********");
        viewHolder.etNewPassword.setText("");
        viewHolder.etConfirmNewPassword.setText("");
    }
    private void updateUserInfo() {
        // TODO: 更新用户信息
    }

}