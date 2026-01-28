package com.anrola.onmyway.Activities;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.anrola.onmyway.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import com.anrola.onmyway.Fragments.MineFragment;
import com.anrola.onmyway.Fragments.NavFragment;
import com.anrola.onmyway.Fragments.OrderFragment;

public class MainActivity extends AppCompatActivity {

    private Fragment orderFragment;  // 接单Fragment
    private Fragment navFragment;   // 导航Fragment
    private Fragment mineFragment;   // 我的Fragment
    private Fragment currentFragment; // 当前显示的Fragment
    private BottomNavigationView bottomNav; // 底部导航栏控件
    private boolean isExitTipShowed = false; // 是否已显示退出提示
    private final long EXIT_INTERVAL = 2000; // 双击间隔（2秒）
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        //String[] sList = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
        //ActivityCompat.requestPermissions(this, sList, 1001);

        bottomNav = findViewById(R.id.ma_bottom_nav);

        initFragments();    // 初始化Fragment
        setBottomNavClickListener();    // 设置底部导航栏点击事件
        bottomNav.setSelectedItemId(R.id.nav_order);    // 设置默认选中项为接单
        setupDoubleClickBackExit();     // 设置双击返回
    }

    // 初始化Fragment
    private void initFragments() {
        if (orderFragment == null) {
            orderFragment = new OrderFragment(); // 接单Fragment
        }
        if (mineFragment == null) {
            mineFragment = new MineFragment();   // 我的Fragment
        }
        if (navFragment == null) {
            navFragment = new NavFragment();    // 导航Fragment
        }
    }

    // 底部导航栏点击事件：切换Fragment
    private void setBottomNavClickListener() {
        bottomNav.setOnItemSelectedListener(new BottomNavigationView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();
                Fragment targetFragment = null;

                // 根据点击的项匹配对应的Fragment
                if (itemId == R.id.nav_order) {
                    targetFragment = orderFragment;
                } else if (itemId == R.id.nav_mine) {
                    targetFragment = mineFragment;
                }else if (itemId == R.id.nav_navigation) {
                    targetFragment = navFragment;
                }

                // 切换Fragment
                if (targetFragment != null && targetFragment != currentFragment) {
                    switchFragment(targetFragment, true);
                    currentFragment = targetFragment;
                    return true;
                }
                return false;
            }
        });
    }

    // Fragment切换方法
    private void switchFragment(Fragment targetFragment, boolean addToBackStack) {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();

        //  设置切换动画
        if (currentFragment == orderFragment ){
            transaction.setCustomAnimations(
                    R.anim.slide_in_right,  // 新Fragment进入动画
                    R.anim.slide_out_left  // 旧Fragment退出动画
            );
        }else if (currentFragment == navFragment ){
            if (targetFragment == orderFragment){
                transaction.setCustomAnimations(
                        R.anim.slide_in_left,
                        R.anim.slide_out_right
                );
            }else if (targetFragment == mineFragment){
                transaction.setCustomAnimations(
                        R.anim.slide_in_right,
                        R.anim.slide_out_left
                );
            }
        }else if (currentFragment == mineFragment ){
            transaction.setCustomAnimations(
                    R.anim.slide_in_left,
                    R.anim.slide_out_right
            );
        }

        // 如果当前Fragment不是目标Fragment，则隐藏
        if (currentFragment != null && currentFragment != targetFragment) {
            transaction.hide(currentFragment);
        }

        // 如果目标Fragment未添加过，先添加，否则直接显示
        if (!targetFragment.isAdded()) {
            transaction.add(R.id.ma_fragment_container, targetFragment);
            // 是否加入回退栈
            if (addToBackStack) {
                transaction.addToBackStack(null);
            }
        } else {
            transaction.show(targetFragment);
        }
        // 提交事务
        transaction.commit();
    }
    private void setupDoubleClickBackExit() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isExitTipShowed) {
                    // 2秒内第二次按返回键：直接退出
                    finish();
                    System.exit(0); // 退出
                } else {
                    // 第一次按返回键：显示提示，2秒后重置标记
                    isExitTipShowed = true;
                    Toast toast = Toast.makeText(MainActivity.this, "再按一次退出应用", Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 60);
                    toast.show();
                    // 2秒后重置标记
                    handler.postDelayed(() -> isExitTipShowed = false, EXIT_INTERVAL);
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);   // 页面销毁时移除Handler回调
    }

    public Fragment getNavFragment() {
        return navFragment;
    }

    public Fragment getOrderFragment() {
        return orderFragment;
    }

    public Fragment getMineFragment() {
        return mineFragment;
    }
}