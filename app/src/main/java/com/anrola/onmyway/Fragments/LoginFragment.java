package com.anrola.onmyway.Fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.anrola.onmyway.Activities.MainActivity;
import com.anrola.onmyway.R;

import org.json.JSONException;
import org.json.JSONObject;

import com.anrola.onmyway.Interface.OnLoginFragmentSwitchListener;
import com.anrola.onmyway.Utils.EditTextController;
import com.anrola.onmyway.Utils.MyRequest;
import com.anrola.onmyway.Utils.SharedPreferencesManager;

public class LoginFragment extends Fragment {

    private static class ViewHolder {
        private Button btnLogin;
        private EditText etName;
        private EditText etPassword;
        private TextView tvGoRegister;  // 注册按钮
        private CheckBox cbRememberPWD;
        private CheckBox cbAutoLogin;
    }
    private ViewHolder viewHolder = new ViewHolder();
    private Context context;
    private Handler handler;
    private OnLoginFragmentSwitchListener switchListener;   // 切换登录注册界面监听器
    private final EditTextController editTextController = new EditTextController();  // 输入框控制器
    private final MyRequest myRequest = new MyRequest();  // 请求类
    private SharedPreferencesManager sharedPreferencesManager;
    private final int LoginHandlerWhat = 1;
    private final String TAG = "LoginFragment";

    @Override
    public void onAttach(@NonNull android.content.Context context) {
        super.onAttach(context);

        // 确保宿主 Activity 实现了接口
        if (context instanceof OnLoginFragmentSwitchListener) {
            switchListener = (OnLoginFragmentSwitchListener) context;
        } else {
            throw new RuntimeException(context + " 必须实现 OnLoginFragmentSwitchListener 接口");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // 加载登录 Fragment 布局
        View view = inflater.inflate(R.layout.fragment_login, container, false);


        initView(view);
        initHandler();
        setClickEvents();
        editTextController.setTextListener(context, viewHolder.etName);
        editTextController.setTextListener(context, viewHolder.etPassword);
        initData();
        return view;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        switchListener = null;
    }
    private void initView(View view) {
        context = view.getContext();
        sharedPreferencesManager = SharedPreferencesManager.getInstance(context);
        viewHolder.tvGoRegister = view.findViewById(R.id.lf_tv_go_register);
        viewHolder.btnLogin = view.findViewById(R.id.lf_btn_login);
        viewHolder.etName = view.findViewById(R.id.lf_et_name);
        viewHolder.etPassword = view.findViewById(R.id.lf_et_password);
        viewHolder.cbRememberPWD = view.findViewById(R.id.lf_cb_remember_pwd);
        viewHolder.cbAutoLogin = view.findViewById(R.id.lf_cb_auto_login);
    }
    private void initHandler() {
        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);

                // 空数据处理
                if (msg.obj == null) {
                    Log.e(TAG, "JSON解析失败：响应字符串为空");
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle("错误")
                            .setMessage("请求失败, 无响应")
                            .setPositiveButton("确定", null)
                            .show();
                    return; // 终止后续解析逻辑
                }


                if (msg.what == LoginHandlerWhat) {
                    // 解析JSON
                    JSONObject response = null;
                    try {
                        response = new JSONObject((String) msg.obj);
                        if (response.getInt("code") == 200) {
                            Toast.makeText(getActivity(), "登录成功", Toast.LENGTH_SHORT).show();

                            String tKey = getString(R.string.shared_preferences_token_key);
                            String nKey = getString(R.string.shared_preferences_user_name_key);
                            String rKey = getString(R.string.shared_preferences_remember_pwd_key);
                            String aKey = getString(R.string.shared_preferences_auto_login_key);
                            String token = response.getJSONObject("data").getString("token");

                            sharedPreferencesManager.save(tKey, token);
                            sharedPreferencesManager.save(nKey, viewHolder.etName.getText().toString());
                            sharedPreferencesManager.save(rKey, viewHolder.cbRememberPWD.isChecked());
                            sharedPreferencesManager.save(aKey, viewHolder.cbAutoLogin.isChecked());

                            if (viewHolder.cbRememberPWD.isChecked()){
                                String  pKey = getString(R.string.shared_preferences_password_key);
                                sharedPreferencesManager.save(pKey, viewHolder.etPassword.getText().toString());
                            }

                            Intent intent = new Intent(getActivity(), MainActivity.class);
                            startActivity(intent);
                        } else if (response.getInt("code") == 500) {
                            Toast.makeText(getActivity(), "用户名或密码错误", Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        };
    }
    private void setClickEvents() {
        // 点击登录
        viewHolder.btnLogin.setOnClickListener(v -> {
            String nameInput = viewHolder.etName.getText().toString();
            String passwordInput = viewHolder.etPassword.getText().toString();
            Boolean isUsernameEmpty = nameInput.isEmpty();
            Boolean isPasswordEmpty = passwordInput.isEmpty();

            if (isUsernameEmpty || isPasswordEmpty) { // 密码或账号为空
                Toast.makeText(getActivity(), "请填写用户名和密码", Toast.LENGTH_SHORT).show();
                if (isUsernameEmpty) {   // 用户名为空
                    // 设置边框闪烁动画
                    editTextController.startBorderWidthBlinkAnimation(
                            context,
                            viewHolder.etName,
                            context.getColor(R.color.red),
                            0, 3, 8, 600, 3);
                }
                if (isPasswordEmpty) {   // 密码为空
                    // 设置边框闪烁动画
                    editTextController.startBorderWidthBlinkAnimation(
                            context,
                            viewHolder.etPassword,
                            context.getColor(R.color.red),
                            0, 3, 8, 600, 3);
                }
                return;
            }

            // 登录逻辑
            JSONObject loginJson = new JSONObject();
            try {
                loginJson.put("name_or_phone", nameInput);
                loginJson.put("password", passwordInput);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }

            myRequest.post(
                    myRequest.getBaseURL(context) + "/user/login",
                    loginJson,
                    handler,
                    LoginHandlerWhat);
        });
        // 点击注册，切换到注册 Fragment
        viewHolder.tvGoRegister.setOnClickListener(v -> {
            if (switchListener != null) {
                switchListener.switchToRegisterFragment();
            }
        });
        // 自动登录点击
        viewHolder.cbAutoLogin.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                viewHolder.cbRememberPWD.setChecked(true);
            }
        });
    }


    private void initData() {
        String nKey = getString(R.string.shared_preferences_user_name_key);
        String rKey = getString(R.string.shared_preferences_remember_pwd_key);
        String aKey = getString(R.string.shared_preferences_auto_login_key);
        if (sharedPreferencesManager.have(nKey)) {
            String userName = sharedPreferencesManager.get(nKey);
            viewHolder.etName.setText(userName);
        }
        if (sharedPreferencesManager.have(rKey)) {
            boolean isRememberPWD = sharedPreferencesManager.getBoolean(rKey);
            if (isRememberPWD) {
                String pKey = getString(R.string.shared_preferences_password_key);
                if (sharedPreferencesManager.have(pKey)) {
                    String password = sharedPreferencesManager.get(pKey);
                    viewHolder.etPassword.setText(password);
                }
            }
            viewHolder.cbRememberPWD.setChecked(isRememberPWD);
        }
        if (sharedPreferencesManager.have(aKey)) {
            boolean isAutoLogin = sharedPreferencesManager.getBoolean(aKey);
            viewHolder.cbAutoLogin.setChecked(isAutoLogin);
            if (isAutoLogin) {
                viewHolder.btnLogin.performClick();
            }
        }
    }
}