package com.example.onmyway.Fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import de.hdodenhof.circleimageview.CircleImageView;

import com.example.onmyway.Activities.MainActivity;
import com.example.onmyway.Activities.UserInfoActivity;
import com.example.onmyway.Adapter.AvatarSelectAdapter;
import com.example.onmyway.Entity.Avatar;
import com.example.onmyway.R;
import com.example.onmyway.Utils.AssetsAvatarManager;
import com.example.onmyway.Utils.MyRequest;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MineFragment extends Fragment {

    private static class ViewHolder {
        // 控件
        private static CircleImageView ivAvatar;
        private static TextView tvNickname;
        private static TextView tvRiderId;
        private static TextView tvEditInfo;
        private static TextView tvTodayIncome;
        private static TextView tvMonthIncome;
        private static TextView tvTotalIncome;
        private static Button btnLogout;
        private static TextView tvMyIncomeDetail;
        private static TextView tvMySetting;
        // 图表相关控件
        private static LineChart incomeChart;
        private static TextView tvWeek;
        private static TextView tvMonth;
    }
    private static class ChartData {
        private static final List<Entry> entriesWeek = new ArrayList<>();
        private static final List<String> xWeekLabels = new ArrayList<>();
        private static int weekTotalIncome = 0;
        private static final List<Entry> entriesMonth = new ArrayList<>();
        private static final List<String> xMonthLabels = new ArrayList<>();
        private static int monthTotalIncome = 0;
        private static int totalIncome = 0;
    }
    private static class HandlerWhats {
        private static final int getWeekIncomeHandlerWhat = 1;
        private static final int getMonthIncomeHandlerWhat = 2;
        private static final int getTotalIncomeHandlerWhat = 3;
        private static final int getUserInfoHandlerWhat = 4;
    }
    private static class UserData {
        private static Long userId;
        private static String userName;
        private static String userNickName;
        private static String userPhone;
        private static int userAvatar;
    }
    private Context context;
    private SharedPreferences sharedPreferences;
    private Handler handler;
    private final MyRequest myRequest = new MyRequest();
    private boolean isWeekData = true; // 当前显示本周/本月数据
    private final String TAG = "MineFragment";


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mine, container, false);

        // 初始化控件
        initView(view);
        // 初始化handler
        initHandler();
        // 初始化图表控件
        initChartView(view);
        // 初始化图表
        initChart();
        // 请求图表数据
        requestChartData();
        // 请求用户数据
        requestUserData();
        // 设置点击事件
        setClickEvents();


        // 加载用户数据
        loadUserData();


        return view;
    }

    // 初始化控件
    private void initView(View view) {
        context = view.getContext();
        sharedPreferences = requireActivity().getSharedPreferences(
                getString(R.string.shared_preferences_user_info_key),
                Context.MODE_PRIVATE);
        ViewHolder.ivAvatar = view.findViewById(R.id.mf_iv_avatar);
        ViewHolder.tvNickname = view.findViewById(R.id.mf_tv_nickname);
        ViewHolder.tvRiderId = view.findViewById(R.id.mf_tv_rider_id);
        ViewHolder.tvEditInfo = view.findViewById(R.id.mf_tv_edit_info);
        ViewHolder.tvTodayIncome = view.findViewById(R.id.mf_tv_today_income);
        ViewHolder.tvMonthIncome = view.findViewById(R.id.mf_tv_month_income);
        ViewHolder.tvTotalIncome = view.findViewById(R.id.mf_tv_total_income);
        ViewHolder.btnLogout = view.findViewById(R.id.mf_btn_logout);
        ViewHolder.tvMyIncomeDetail = view.findViewById(R.id.mf_tv_my_income_detail);
        ViewHolder.tvMySetting = view.findViewById(R.id.mf_tv_my_setting);
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





                // 响应数据处理
                JSONObject response = null;
                switch (msg.what) {
                    case HandlerWhats.getWeekIncomeHandlerWhat:  // 获取本周收入
                        try {
                            response = new JSONObject((String) msg.obj);
                            JSONObject data = response.getJSONObject("data");
                            JSONObject weekDailyIncome = data.getJSONObject("income");
                            ChartData.weekTotalIncome = data.getInt("total");
                            if (response.getInt("code") == 200) {
                                int length = weekDailyIncome.length();
                                for (int i = 0; i < length; i++) {
                                    int amount = weekDailyIncome.getInt(String.valueOf(i + 1));
                                    ChartData.entriesWeek.add(new Entry(i, amount));
                                    ChartData.xWeekLabels.add("周" + String.valueOf(i + 1));
                                }
                                loadChartData(isWeekData, ChartData.entriesWeek, ChartData.xWeekLabels);
                                Log.d(TAG, ChartData.entriesWeek.toString());
                                ViewHolder.tvTodayIncome.setText(String.format("¥%s", ChartData.weekTotalIncome));
                            } else if (response.getInt("code") == 500) {
                                Toast.makeText(getActivity(), "收入获取错误", Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                        break;
                    case HandlerWhats.getMonthIncomeHandlerWhat:  // 获取本月收入
                        try {
                            response = new JSONObject((String) msg.obj);
                            JSONObject data = response.getJSONObject("data");
                            JSONObject monthDailyIncome = data.getJSONObject("income");
                            ChartData.monthTotalIncome = data.getInt("total");
                            if (response.getInt("code") == 200) {
                                int length = monthDailyIncome.length();
                                for (int i = 0; i < length; i++) {
                                    int amount = monthDailyIncome.getInt(String.valueOf(i + 1));
                                    ChartData.entriesMonth.add(new Entry(i, amount));
                                    ChartData.xMonthLabels.add(String.valueOf(i + 1) + "日");
                                }
                                Log.d(TAG, ChartData.entriesWeek.toString());
                                ViewHolder.tvMonthIncome.setText(String.format("¥%s", ChartData.monthTotalIncome));
                            } else if (response.getInt("code") == 500) {
                                Toast.makeText(getActivity(), "收入获取错误", Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                        break;
                    case HandlerWhats.getTotalIncomeHandlerWhat: // 获取总收入
                        try {
                            response = new JSONObject((String) msg.obj);
                            if (response.getInt("code") == 200) {
                                JSONObject data = response.getJSONObject("data");
                                ChartData.totalIncome = data.getInt("income");
                                ViewHolder.tvTotalIncome.setText(String.format("¥%s", ChartData.totalIncome));
                            } else if (response.getInt("code") == 500) {
                                Toast.makeText(getActivity(), "收入获取错误", Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                        break;
                    case HandlerWhats.getUserInfoHandlerWhat:    // 获取用户信息
                        try {
                            response = new JSONObject((String) msg.obj);
                            if (response.getInt("code") == 200) {
                                JSONObject data = response.getJSONObject("data");
                                UserData.userId = data.getLong("id");
                                UserData.userName = data.getString("name");
                                UserData.userNickName = data.getString("nickname");
                                UserData.userPhone = data.getString("phone");
                                UserData.userAvatar = data.getInt("avatar");
                                ViewHolder.tvNickname.setText(UserData.userNickName);
                                ViewHolder.tvRiderId.setText(String.format("ID: %s", UserData.userId));
                                AssetsAvatarManager avatarManager = new AssetsAvatarManager(getActivity());
                                List<Bitmap> avatars = avatarManager.getAllAvatars();
                                ViewHolder.ivAvatar.setImageBitmap(avatars.get(UserData.userAvatar));
                            } else if (response.getInt("code") == 500) {
                                Toast.makeText(getActivity(), "用户信息获取错误", Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                        break;
                }
            }
        };
    }

    // 设置点击事件
    private void setClickEvents() {

        // 头像点击
        ViewHolder.ivAvatar.setOnClickListener(v -> {
            showAvatarSelectDialog();
        });
        // 修改信息点击
        ViewHolder.tvEditInfo.setOnClickListener(v->{
            Intent intent = new Intent(getActivity(), UserInfoActivity.class);
            startActivity(intent);
        });
        // 登出点击
        // 显示登出确认对话框
        ViewHolder.btnLogout.setOnClickListener(v -> showLogoutDialog());
        // 收益详情
        // TODO: 跳转到收益详情页面
        ViewHolder.tvMyIncomeDetail.setOnClickListener(v -> Toast.makeText(getActivity(), "查看我的收益详情", Toast.LENGTH_SHORT).show());
        // 设置点击
        // TODO: 跳转到系统设置页面
        ViewHolder.tvMySetting.setOnClickListener(v -> Toast.makeText(getActivity(), "进入系统设置", Toast.LENGTH_SHORT).show());

        // 本周/本月切换
        ViewHolder.tvWeek.setOnClickListener(v -> {
            if (!isWeekData) {
                isWeekData = true;
                updateTabStyle();
                loadChartData(isWeekData, ChartData.entriesWeek, ChartData.xWeekLabels);
            }
        });
        ViewHolder.tvMonth.setOnClickListener(v -> {
            if (isWeekData) {
                isWeekData = false;
                updateTabStyle();
                loadChartData(isWeekData, ChartData.entriesMonth, ChartData.xMonthLabels);
            }
        });
    }

    // 更新切换按钮样式（选中/未选中）
    private void updateTabStyle() {
        if (isWeekData) {
            ViewHolder.tvWeek.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_light));
            ViewHolder.tvWeek.setTextColor(getResources().getColor(android.R.color.white));
            ViewHolder.tvMonth.setBackgroundColor(getResources().getColor(android.R.color.white));
            ViewHolder.tvMonth.setTextColor(getResources().getColor(android.R.color.darker_gray));
        } else {
            ViewHolder.tvMonth.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_light));
            ViewHolder.tvMonth.setTextColor(getResources().getColor(android.R.color.white));
            ViewHolder.tvWeek.setBackgroundColor(getResources().getColor(android.R.color.white));
            ViewHolder.tvWeek.setTextColor(getResources().getColor(android.R.color.darker_gray));
        }
    }

    // 初始化图表控件
    private void initChartView(View view) {
        ViewHolder.incomeChart = view.findViewById(R.id.mf_income_chart);
        ViewHolder.tvWeek = view.findViewById(R.id.mf_tv_week);
        ViewHolder.tvMonth = view.findViewById(R.id.mf_tv_month);
    }

    // 初始化图表样式
    private void initChart() {
        // 隐藏描述文字
        Description description = new Description();
        description.setEnabled(false);
        ViewHolder.incomeChart.setDescription(description);

        // 隐藏图例
        ViewHolder.incomeChart.getLegend().setEnabled(false);

        // 禁用触摸缩放
        ViewHolder.incomeChart.setScaleEnabled(false);

        // X轴配置
        XAxis xAxis = ViewHolder.incomeChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false); // 隐藏X轴网格线

        // Y轴配置
        YAxis leftYAxis = ViewHolder.incomeChart.getAxisLeft();
        leftYAxis.setDrawGridLines(true);
        leftYAxis.setGridColor(getResources().getColor(android.R.color.darker_gray));
        ViewHolder.incomeChart.getAxisRight().setEnabled(false); // 隐藏右侧Y轴

        // 禁用图表点击
        ViewHolder.incomeChart.setClickable(false);
    }

    private void requestChartData() {
        String key = getString(R.string.shared_preferences_token_key);
        String weekUrl = myRequest.getBaseURL(context) + "/income/getWeekDailyIncomeByUserId";
        String monthUrl = myRequest.getBaseURL(context) + "/income/getMonthDailyIncomeByUserId";
        String token = sharedPreferences.getString(key, null);
        // 获取本周/本月收益数据
        myRequest.get(weekUrl,
                handler,
                HandlerWhats.getWeekIncomeHandlerWhat,
                token);
        myRequest.get(monthUrl,
                handler,
                HandlerWhats.getMonthIncomeHandlerWhat,
                token);
    }

    private void requestUserData() {
        String key = getString(R.string.shared_preferences_token_key);
        String totalIncomeUrl = myRequest.getBaseURL(context) + "/income/getTotalIncomeByUserId";
        String getUserInfoUrl = myRequest.getBaseURL(context) + "/user/id";
        String token = sharedPreferences.getString(key, null);
        myRequest.get(totalIncomeUrl,
                handler,
                HandlerWhats.getTotalIncomeHandlerWhat,
                token);
        myRequest.get(getUserInfoUrl,
                handler,
                HandlerWhats.getUserInfoHandlerWhat,
                token);
    }


    // 加载图表数据（本周/本月）
    private void loadChartData(boolean isWeek, List<Entry> entries, List<String> xLabels) {
        // 构建数据集
        LineDataSet dataSet = new LineDataSet(entries, "收益");
        dataSet.setColor(getResources().getColor(android.R.color.holo_orange_dark)); // 折线颜色
        dataSet.setCircleColor(getResources().getColor(android.R.color.holo_orange_dark)); // 节点颜色
        dataSet.setCircleRadius(4f); // 节点大小
        dataSet.setDrawFilled(true); // 填充折线下方区域
        dataSet.setFillColor(getResources().getColor(android.R.color.holo_orange_light)); // 填充颜色
        dataSet.setFillAlpha(80); // 填充透明度
        dataSet.setLineWidth(2f); // 折线宽度

        // 构建LineData
        List<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(dataSet);
        LineData lineData = new LineData(dataSets);

        // 设置X轴标签
        ViewHolder.incomeChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(xLabels));

        // 设置数据并刷新图表
        ViewHolder.incomeChart.setData(lineData);
        ViewHolder.incomeChart.invalidate(); // 刷新
    }

    // 加载用户数据
    private void loadUserData() {
    }

    // 退出登录对话框
    private void showLogoutDialog() {
        new AlertDialog.Builder(getActivity())
                .setTitle("退出登录")
                .setMessage("确定要退出当前账号吗？")
                .setPositiveButton("确定", (dialog, which) -> {
                    Toast.makeText(getActivity(), "退出登录成功", Toast.LENGTH_SHORT).show();
                    // 实际项目中添加退出登录逻辑
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showAvatarSelectDialog() {
        // 构建弹窗
        AssetsAvatarManager avatarManager = new AssetsAvatarManager(context);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_avatar_select, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); // 设置背景透明

        // 获取头像资源
        List<Bitmap> avatars = avatarManager.getAllAvatars();
        // 初始化头像列表数据
        List<Avatar> avatarList = new ArrayList<>();
        for (int i = 0; i < avatars.size(); i++) {
            Avatar avatar = new Avatar(avatars.get(i), false);
            avatarList.add(avatar);
        }
        // 初始化GridView和适配器
        GridView gvAvatar = dialogView.findViewById(R.id.gv_avatar_list);
        AvatarSelectAdapter adapter = new AvatarSelectAdapter(context, avatarList);
        gvAvatar.setAdapter(adapter);

        //设置默认选中头像
        adapter.setSelectedPosition(UserData.userAvatar);

        // 头像列表点击事件
        gvAvatar.setOnItemClickListener((parent, view, position, id) -> {
            // 更新选中状态
            adapter.setSelectedPosition(position);
        });

        // 确认更换按钮点击事件
        dialogView.findViewById(R.id.btn_confirm_change).setOnClickListener(v -> {
            // 获取选中的头像位置
            int selectedPos = adapter.getSelectedPosition();
            // TODO 更新头像数据库
            // 更新主界面头像
            ViewHolder.ivAvatar.setImageBitmap(avatars.get(selectedPos));
            // 提示更换成功
            Toast.makeText(context, "头像更换成功", Toast.LENGTH_SHORT).show();
            // 关闭弹窗
            dialog.dismiss();
        });

        // 显示弹窗
        dialog.show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        myRequest.closeStream();
    }
}