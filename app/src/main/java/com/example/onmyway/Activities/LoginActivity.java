package com.example.onmyway.Activities;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.onmyway.R;

import com.example.onmyway.Fragments.LoginFragment;
import com.example.onmyway.Fragments.RegisterFragment;
import com.example.onmyway.Interface.OnLoginFragmentSwitchListener;

public class LoginActivity extends AppCompatActivity implements OnLoginFragmentSwitchListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        //启动LoginFragment
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        LoginFragment loginFragment = new LoginFragment();
        fragmentTransaction.add(R.id.la_fl_fragment_main_zone, loginFragment);
        fragmentTransaction.commit();



    }

    @Override
    public void switchToRegisterFragment() {
        replaceFragment(new RegisterFragment());
    }

    @Override
    public void switchToLoginFragment() {
        replaceFragment(new LoginFragment());
    }

    private void replaceFragment(Fragment targetFragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();



        // 切换动画
        transaction.setCustomAnimations(
                R.anim.slide_in_right,  // 进入动画（从右滑入）
                R.anim.slide_out_left,  // 退出动画（向左滑出）
                R.anim.slide_in_left,   // 返回时进入动画
                R.anim.slide_out_right  // 返回时退出动画
        );
        // 替换容器中的 Fragment , 销毁旧 Fragment 避免叠加
        transaction.replace(R.id.la_fl_fragment_main_zone, targetFragment);
        // 加入回退栈, 按返回键切回上一个 Fragment
        transaction.addToBackStack(null);

        // 提交事务
        transaction.commit();
    }
}