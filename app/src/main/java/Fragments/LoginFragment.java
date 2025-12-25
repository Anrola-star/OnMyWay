package Fragments;

import static Utils.Unit.*;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.onmyway.R;

import Interface.OnLoginFragmentSwitchListener;

public class LoginFragment extends Fragment {

    private Context context;
    private OnLoginFragmentSwitchListener switchListener;// 切换登录注册界面监听器
    private Button btnLogin;
    private EditText etUsername;
    private EditText etPassword;
    private TextView tvGoRegister;  // 注册按钮
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
        setClickEvents();
        setTextListener(etUsername);
        setTextListener(etPassword);

        return view;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        switchListener = null;
    }

    void initView(View view) {
        context = view.getContext();
        tvGoRegister = view.findViewById(R.id.lf_tv_go_register);
        btnLogin = view.findViewById(R.id.lf_btn_login);
        etUsername = view.findViewById(R.id.lf_et_username);
        etPassword = view.findViewById(R.id.lf_et_password);
    }

    void setClickEvents(){
        // 点击登录
        btnLogin.setOnClickListener(v -> {
            String usernameInput = etUsername.getText().toString();
            String passwordInput = etPassword.getText().toString();
            Boolean isUsernameEmpty = usernameInput.isEmpty();
            Boolean isPasswordEmpty = passwordInput.isEmpty();

            if(isUsernameEmpty || isPasswordEmpty){
                Toast.makeText(getActivity(), "请填写用户名和密码", Toast.LENGTH_SHORT).show();
                if (isUsernameEmpty){
                    //setEditTextErrorBorder(etUsername, true);
                    startBorderBlinkAnimation(etUsername);
                }
                if (isPasswordEmpty){
                    //setEditTextErrorBorder(etPassword, true);
                    startBorderBlinkAnimation(etPassword);
                }
            }
        });

        // 点击注册，切换到注册 Fragment
        tvGoRegister.setOnClickListener(v -> {
            if (switchListener != null) {
                switchListener.switchToRegisterFragment();
            }
        });
    }

    // 设置错误输入框边框
    private void setEditTextErrorBorder(EditText editText, boolean isError){
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.RECTANGLE);
        //shape.setColor(Color.WHITE);

        int borderColor;    // 边框颜色
        int borderWidth;    // 边框宽度
        if (isError) {
            borderColor = Color.parseColor("#FF3333");
            borderWidth = dp2px(context, 1);
        } else {
            borderColor = Color.parseColor("#CCCCCC");
            borderWidth = 0;
        }

        shape.setStroke(borderWidth, borderColor);
        shape.setCornerRadius(dp2px(context,5));
        editText.setBackground(shape);
    }

    // 设置输入框内容改变监听器
    private void setTextListener(EditText et) {
        et.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // 输入内容时，强制恢复正常边框
                setEditTextErrorBorder(et, false);
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
        et.setOnFocusChangeListener((v, hasFocus) -> {
            // 获取焦点时，强制恢复正常边框
            if (hasFocus) {
                setEditTextErrorBorder(et, false);
            }
        });
    }

    // 闪烁边框
    private void startBorderBlinkAnimation(EditText et) {
        // 创建渐变Drawable
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.RECTANGLE);
        //shape.setColor(Color.WHITE);
        shape.setCornerRadius(dp2px(context,8)); // 圆角

        // 定义颜色动画：透明红 → 纯红 → 透明红 → 纯红 → 白  闪烁2次
        ValueAnimator colorAnimator = ValueAnimator.ofArgb(
                Color.parseColor("#80FF3333"), // 半透明红
                Color.parseColor("#FF3333"),   // 纯红
                Color.parseColor("#80FF3333"), // 半透明红
                Color.parseColor("#FF3333"),    // 纯红
                Color.parseColor("#FFFFFF")   // 白色
        );
        // 创建宽度动画：0dp → 2dp → 0dp → 2dp
        ValueAnimator widthAnimator = ValueAnimator.ofInt(
                0,
                dp2px(context, 3),
                0,
                dp2px(context, 3),
                0
        );

        colorAnimator.setDuration(600); // 总时长600ms
        widthAnimator.setDuration(600);

        colorAnimator.addUpdateListener(animation -> {
            // 动画过程中更新边框颜色
            int currentColor = (int) animation.getAnimatedValue();
            shape.setStroke(dp2px(context,2), currentColor); // 边框宽度2dp
            et.setBackground(shape);
        });

        widthAnimator.addUpdateListener(animation -> {
            int currentWidth = (int) animation.getAnimatedValue();
            shape.setStroke(currentWidth, Color.parseColor("#FF3333"));
            et.setBackground(shape);
        });
        colorAnimator.start();
        widthAnimator.start();
    }
}