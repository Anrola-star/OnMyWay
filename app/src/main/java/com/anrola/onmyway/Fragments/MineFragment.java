package com.anrola.onmyway.Fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
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
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import de.hdodenhof.circleimageview.CircleImageView;

import com.anrola.onmyway.Activities.UserInfoActivity;
import com.anrola.onmyway.Adapter.AvatarSelectAdapter;
import com.anrola.onmyway.Entity.Avatar;
import com.anrola.onmyway.R;
import com.anrola.onmyway.Utils.AssetsAvatarManager;
import com.anrola.onmyway.Utils.MyRequest;
import com.anrola.onmyway.Utils.SharedPreferencesManager;
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
        private CircleImageView ivAvatar;
        private TextView tvNickname;
        private TextView tvRiderId;
        private TextView tvEditInfo;
        private TextView tvTodayIncome;
        private TextView tvMonthIncome;
        private TextView tvTotalIncome;
        private Button btnLogout;
        private TextView tvMyIncomeDetail;
        private TextView tvMySetting;
        // 图表相关控件
        private LineChart incomeChart;
        private TextView tvWeek;
        private TextView tvMonth;
    }
    private ViewHolder viewHolder = new ViewHolder();
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
    private SharedPreferencesManager sharedPreferencesManager;
    private Handler handler;
    private final MyRequest myRequest = new MyRequest();
    private boolean isWeekData = true; // 当前显示本周/本月数据
    private boolean isMonthDataGetted = false;
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
        requestChartData(true);
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
        sharedPreferencesManager = SharedPreferencesManager.getInstance(context);
        viewHolder.ivAvatar = view.findViewById(R.id.mf_iv_avatar);
        viewHolder.tvNickname = view.findViewById(R.id.mf_tv_nickname);
        viewHolder.tvRiderId = view.findViewById(R.id.mf_tv_rider_id);
        viewHolder.tvEditInfo = view.findViewById(R.id.mf_tv_edit_info);
        viewHolder.tvTodayIncome = view.findViewById(R.id.mf_tv_today_income);
        viewHolder.tvMonthIncome = view.findViewById(R.id.mf_tv_month_income);
        viewHolder.tvTotalIncome = view.findViewById(R.id.mf_tv_total_income);
        viewHolder.btnLogout = view.findViewById(R.id.mf_btn_logout);
        viewHolder.tvMyIncomeDetail = view.findViewById(R.id.mf_tv_my_income_detail);
        viewHolder.tvMySetting = view.findViewById(R.id.mf_tv_my_setting);
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
                                viewHolder.tvTodayIncome.setText(String.format("¥%s", ChartData.weekTotalIncome));
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
                                viewHolder.tvMonthIncome.setText(String.format("¥%s", ChartData.monthTotalIncome));
                                isMonthDataGetted = true;
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
                                viewHolder.tvTotalIncome.setText(String.format("¥%s", ChartData.totalIncome));
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
                                viewHolder.tvNickname.setText(UserData.userNickName);
                                viewHolder.tvRiderId.setText(String.format("ID: %s", UserData.userId));
                                AssetsAvatarManager avatarManager = new AssetsAvatarManager(getActivity());
                                List<Bitmap> avatars = avatarManager.getAllAvatars();
                                viewHolder.ivAvatar.setImageBitmap(avatars.get(UserData.userAvatar));
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
        viewHolder.ivAvatar.setOnClickListener(v -> {
            showAvatarSelectDialog();
        });
        // 修改信息点击
        viewHolder.tvEditInfo.setOnClickListener(v->{
            Intent intent = new Intent(getActivity(), UserInfoActivity.class);
            startActivity(intent);
        });
        // 登出点击
        // 显示登出确认对话框
        viewHolder.btnLogout.setOnClickListener(v -> showLogoutDialog());
        // 收益详情
        // TODO: 跳转到收益详情页面
        viewHolder.tvMyIncomeDetail.setOnClickListener(v -> Toast.makeText(getActivity(), "查看我的收益详情", Toast.LENGTH_SHORT).show());
        // 设置点击
        // TODO: 跳转到系统设置页面
        viewHolder.tvMySetting.setOnClickListener(v -> Toast.makeText(getActivity(), "进入系统设置", Toast.LENGTH_SHORT).show());

        // 本周/本月切换
        viewHolder.tvWeek.setOnClickListener(v -> {
            if (!isWeekData) {
                isWeekData = true;
                updateTabStyle();
                loadChartData(isWeekData, ChartData.entriesWeek, ChartData.xWeekLabels);
            }
        });

        viewHolder.tvMonth.setOnClickListener(v -> {
            if (isWeekData) {

                if (!isMonthDataGetted){
                    requestChartData(false);
                    new Thread(() -> {
                        while (!isMonthDataGetted){
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }
                        isWeekData = false;
                        updateTabStyle();
                        loadChartData(isWeekData, ChartData.entriesMonth, ChartData.xMonthLabels);
                    }).start();
                }

                isWeekData = false;
                updateTabStyle();
                loadChartData(isWeekData, ChartData.entriesMonth, ChartData.xMonthLabels);
            }
        });
    }

    // 更新切换按钮样式（选中/未选中）
    private void updateTabStyle() {
        if (isWeekData) {
            viewHolder.tvWeek.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_light));
            viewHolder.tvWeek.setTextColor(getResources().getColor(android.R.color.white));
            viewHolder.tvMonth.setBackgroundColor(getResources().getColor(android.R.color.white));
            viewHolder.tvMonth.setTextColor(getResources().getColor(android.R.color.darker_gray));
        } else {
            viewHolder.tvMonth.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_light));
            viewHolder.tvMonth.setTextColor(getResources().getColor(android.R.color.white));
            viewHolder.tvWeek.setBackgroundColor(getResources().getColor(android.R.color.white));
            viewHolder.tvWeek.setTextColor(getResources().getColor(android.R.color.darker_gray));
        }
    }

    // 初始化图表控件
    private void initChartView(View view) {
        viewHolder.incomeChart = view.findViewById(R.id.mf_income_chart);
        viewHolder.tvWeek = view.findViewById(R.id.mf_tv_week);
        viewHolder.tvMonth = view.findViewById(R.id.mf_tv_month);
    }

    // 初始化图表样式
    private void initChart() {
        // 隐藏描述文字
        Description description = new Description();
        description.setEnabled(false);
        viewHolder.incomeChart.setDescription(description);

        // 隐藏图例
        viewHolder.incomeChart.getLegend().setEnabled(false);

        // 禁用触摸缩放
        viewHolder.incomeChart.setScaleEnabled(false);

        // X轴配置
        XAxis xAxis = viewHolder.incomeChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false); // 隐藏X轴网格线

        // Y轴配置
        YAxis leftYAxis = viewHolder.incomeChart.getAxisLeft();
        leftYAxis.setDrawGridLines(true);
        leftYAxis.setGridColor(getResources().getColor(android.R.color.darker_gray));
        viewHolder.incomeChart.getAxisRight().setEnabled(false); // 隐藏右侧Y轴

        // 禁用图表点击
        viewHolder.incomeChart.setClickable(false);
    }

    private void requestChartData(boolean isWeekData) {
        // 获取本周/本月收益数据
        String key = getString(R.string.shared_preferences_token_key);
        String token = sharedPreferencesManager.get(key);
        if (isWeekData){
            String weekUrl = myRequest.getBaseURL(context) + "/income/getWeekDailyIncomeByUserId";
            myRequest.get(weekUrl,
                    handler,
                    HandlerWhats.getWeekIncomeHandlerWhat,
                    token);
        }else {
            String monthUrl = myRequest.getBaseURL(context) + "/income/getMonthDailyIncomeByUserId";
            myRequest.get(monthUrl,
                    handler,
                    HandlerWhats.getMonthIncomeHandlerWhat,
                    token);
        }
    }

    private void requestUserData() {
        String key = getString(R.string.shared_preferences_token_key);
        String totalIncomeUrl = myRequest.getBaseURL(context) + "/income/getTotalIncomeByUserId";
        String getUserInfoUrl = myRequest.getBaseURL(context) + "/user/id";
        String token = sharedPreferencesManager.get(key);
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
        viewHolder.incomeChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(xLabels));

        // 设置数据并刷新图表
        viewHolder.incomeChart.setData(lineData);
        viewHolder.incomeChart.invalidate(); // 刷新
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
            viewHolder.ivAvatar.setImageBitmap(avatars.get(selectedPos));
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