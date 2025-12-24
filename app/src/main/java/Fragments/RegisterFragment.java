package Fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.onmyway.R;

import Interface.OnLoginFragmentSwitchListener;

public class RegisterFragment extends Fragment {



    private OnLoginFragmentSwitchListener switchListener;// 切换登录注册界面监听器

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
        // 加载登录 Fragment 布局（替换为你的登录布局文件名）
        View view = inflater.inflate(R.layout.fragment_register, container, false);

        // 注册按钮
        TextView tvGoRegister = view.findViewById(R.id.fr_tv_go_login);

        // 点击注册，切换到注册 Fragment
        tvGoRegister.setOnClickListener(v -> {
            if (switchListener != null) {
                switchListener.switchToLoginFragment();
            }
        });

        return view;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        switchListener = null;
    }
}