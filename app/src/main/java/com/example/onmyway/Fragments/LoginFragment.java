package com.example.onmyway.Fragments;

import android.app.AlertDialog;
import android.content.Context;
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
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.onmyway.R;

import org.json.JSONException;
import org.json.JSONObject;

import com.example.onmyway.Interface.OnLoginFragmentSwitchListener;
import com.example.onmyway.Utils.EditTextController;
import com.example.onmyway.Utils.MyRequest;

public class LoginFragment extends Fragment {

    private Context context;
    private Handler handler;
    private OnLoginFragmentSwitchListener switchListener;   // 切换登录注册界面监听器
    private final EditTextController editTextController = new EditTextController();  // 输入框控制器
    private final MyRequest myRequest = new MyRequest();  // 请求类
    private Button btnLogin;
    private EditText etName;
    private EditText etPassword;
    private TextView tvGoRegister;  // 注册按钮


    private final int LoginHandlerWhat = 1;

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
        //initHandler();
        //setClickEvents();
        //editTextController.setTextListener(context, etName);
        //editTextController.setTextListener(context, etPassword);

        return view;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        switchListener = null;
    }

    private void initView(View view) {
        context = view.getContext();
        tvGoRegister = view.findViewById(R.id.lf_tv_go_register);
        btnLogin = view.findViewById(R.id.lf_btn_login);
        etName = view.findViewById(R.id.lf_et_name);
        etPassword = view.findViewById(R.id.lf_et_password);
    }

    private void initHandler() {
        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);

                // 空数据处理
                if (msg.obj == null) {
                    Log.e("LoginError", "JSON解析失败：响应字符串为空");
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle("错误")
                            .setMessage("请求失败, 无响应")
                            .setPositiveButton("确定", null)
                            .show();
                    //Toast.makeText(getActivity(), "登录失败, 无响应", Toast.LENGTH_SHORT).show();
                    return; // 终止后续解析逻辑
                }


                if (msg.what == LoginHandlerWhat) {
                    // 解析JSON
                    JSONObject response = null;
                    try {
                        response = new JSONObject((String) msg.obj);
                        if (response.getInt("code") == 200) {
                            Toast.makeText(getActivity(), "登录成功", Toast.LENGTH_SHORT).show();
                        } else if (response.getInt("code") == 400) {
                            Toast.makeText(getActivity(), "用户名或密码错误", Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        };
    }

    // 设置监听器
    private void setClickEvents() {
        // 点击登录
        btnLogin.setOnClickListener(v -> {
            String nameInput = etName.getText().toString();
            String passwordInput = etPassword.getText().toString();
            Boolean isUsernameEmpty = nameInput.isEmpty();
            Boolean isPasswordEmpty = passwordInput.isEmpty();

            if (isUsernameEmpty || isPasswordEmpty) { // 密码或账号为空
                Toast.makeText(getActivity(), "请填写用户名和密码", Toast.LENGTH_SHORT).show();
                if (isUsernameEmpty) {   // 用户名为空
                    // 设置边框闪烁动画
                    editTextController.startBorderWidthBlinkAnimation(
                            context,
                            etName,
                            "#FF3333",
                            0, 3, 8, 600, 3);
                }
                if (isPasswordEmpty) {   // 密码为空
                    // 设置边框闪烁动画
                    editTextController.startBorderWidthBlinkAnimation(
                            context,
                            etPassword,
                            "#FF3333",
                            0, 3, 8, 600, 3);
                }
                return;
            }

            // 登录逻辑
            JSONObject loginJson = new JSONObject();
            try {
                loginJson.put("name", nameInput);
                loginJson.put("password", passwordInput);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }

            myRequest.post(
                    "http://100.2.15.232:8080/user/login",
                    loginJson,
                    handler,
                    LoginHandlerWhat);
        });

        // 点击注册，切换到注册 Fragment
        tvGoRegister.setOnClickListener(v -> {
            if (switchListener != null) {
                switchListener.switchToRegisterFragment();
            }
        });
    }
}