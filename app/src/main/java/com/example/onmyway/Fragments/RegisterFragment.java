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

import java.util.ArrayList;

import com.example.onmyway.Interface.OnLoginFragmentSwitchListener;
import com.example.onmyway.Utils.EditTextController;
import com.example.onmyway.Utils.MyRequest;

public class RegisterFragment extends Fragment {

    private Context context;
    private Handler handler;
    private OnLoginFragmentSwitchListener switchListener;   // 切换登录注册界面监听器
    private final EditTextController editTextController = new EditTextController();  // 输入框控制器
    private final MyRequest myRequest = new MyRequest();  // 请求类
    private TextView tvGoLogin;
    private Button btnRegister;
    private EditText etName;
    private EditText etNickName;
    private EditText etEmail;
    private EditText etPassword;
    private EditText etConfirmPassword;
    private ArrayList<EditText> editTexts;

    private final int RegisterHandlerWhat = 1;

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
        // 加载注册 Fragment 布局
        View view = inflater.inflate(R.layout.fragment_register, container, false);

        initViews(view);
        initHandler();
        setTextListener();
        setClickEvents();
        return view;
    }

    private void initViews(View view) {
        context = view.getContext();
        tvGoLogin = view.findViewById(R.id.rf_tv_go_login);
        btnRegister = view.findViewById(R.id.rf_btn_register);
        etName = view.findViewById(R.id.rf_et_register_name);
        etNickName = view.findViewById(R.id.rf_et_register_nickname);
        etEmail = view.findViewById(R.id.rf_et_register_email);
        etPassword = view.findViewById(R.id.rf_et_register_pwd);
        etConfirmPassword = view.findViewById(R.id.rf_et_register_pwd_confirm);


        editTexts = new ArrayList<>();
        editTexts.add(etName);
        editTexts.add(etNickName);
        editTexts.add(etEmail);
        editTexts.add(etPassword);
        editTexts.add(etConfirmPassword);
    }

    private void initHandler(){
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

                if (msg.what == RegisterHandlerWhat) {
                    // 解析JSON
                    JSONObject response = null;
                    try {
                        response = new JSONObject((String) msg.obj);
                        if (response.getInt("code") == 200) {
                            Toast.makeText(getActivity(), "注册成功，返回登录", Toast.LENGTH_SHORT).show();
                            switchListener.switchToLoginFragment();
                        } else if (response.getInt("code") == 400) {
                            Toast.makeText(getActivity(), "注册失败", Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                }

            }
        };
    }
    private void setClickEvents(){
        // 点击注册
        btnRegister.setOnClickListener(v -> {

            String nameInput = etName.getText().toString();
            String nicknameInput = etNickName.getText().toString();
            String emailInput = etEmail.getText().toString();
            String passwordInput = etPassword.getText().toString();
            String confirmPasswordInput = etConfirmPassword.getText().toString();

            ArrayList<Boolean> isInputEmpty = new ArrayList<>();
            isInputEmpty.add(nameInput.isEmpty());
            isInputEmpty.add(nicknameInput.isEmpty());
            isInputEmpty.add(emailInput.isEmpty());
            isInputEmpty.add(passwordInput.isEmpty());
            isInputEmpty.add(confirmPasswordInput.isEmpty());

            // 判断输入框是否为空
            boolean haveEmptyInput = false;

            for (int i = 0; i < editTexts.size(); i++) {
                if (isInputEmpty.get(i)) {
                    haveEmptyInput = true;
                    editTextController.startBorderWidthBlinkAnimation(
                            context,
                            editTexts.get(i),
                            context.getColor(R.color.red),
                            0,
                            3,
                            8,
                            600,
                            3
                    );
                    editTexts.get(i).setError("请输入内容");
                }else{
                    editTextController.setEditTextErrorBorder(
                            context, editTexts.get(i),
                            3,8,false);
                }
            }
            if (haveEmptyInput){
                return;
            }

            // 判断密码是否一致
            if (!passwordInput.equals(confirmPasswordInput)){
                editTextController.startBorderWidthBlinkAnimation(
                        context,
                        etConfirmPassword,
                        context.getColor(R.color.red),
                        0,
                        3,
                        8,
                        600,
                        3
                );
                etConfirmPassword.setError("密码不一致");
                return;
            }

            // 发送注册请求
            JSONObject registerJson = new JSONObject();
            try {
                registerJson.put("name", nameInput);
                registerJson.put("nickname", nicknameInput);
                registerJson.put("email", emailInput);
                registerJson.put("password", passwordInput);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }

            myRequest.post(
                    "http://192.168.31.100:8080/api/register",
                    registerJson,
                    handler,
                    RegisterHandlerWhat
            );
        });

        // 点击登录
        tvGoLogin.setOnClickListener(v -> {
            if (switchListener != null) {
                switchListener.switchToLoginFragment();
            }
        });
    }

    private void setTextListener(){
        editTextController.setTextListener(context, etName);
        editTextController.setTextListener(context, etNickName);
        editTextController.setTextListener(context, etEmail);
        editTextController.setTextListener(context, etPassword);
        editTextController.setTextListener(context, etConfirmPassword);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        switchListener = null;
    }
}