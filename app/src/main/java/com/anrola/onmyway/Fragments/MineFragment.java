package com.anrola.onmyway.Fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import de.hdodenhof.circleimageview.CircleImageView;

import com.anrola.onmyway.Activities.MainActivity;
import com.anrola.onmyway.Activities.UserInfoActivity;
import com.anrola.onmyway.Adapter.AvatarSelectAdapter;
import com.anrola.onmyway.Entity.Avatar;
import com.anrola.onmyway.R;
import com.anrola.onmyway.Utils.AIApiClient;
import com.anrola.onmyway.Utils.AssetsAvatarManager;
import com.anrola.onmyway.Utils.MyRequest;
import com.anrola.onmyway.Utils.SharedPreferencesManager;
import com.anrola.onmyway.Utils.ToastManager;
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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MineFragment extends Fragment {

    private static class ViewHolder {
        private LinearLayout root;
        // 控件
        private CircleImageView ivAvatar;
        private TextView tvNickname;
        private TextView tvRiderId;
        private TextView tvEditInfo;
        private TextView tvWeekIncome;
        private TextView tvMonthIncome;
        private TextView tvTotalIncome;
        private Button btnLogout;
        private TextView tvMyIncomeDetail;
        private TextView tvMySetting;
        // 图表相关控件
        private LineChart incomeChart;
        private TextView tvWeek;
        private TextView tvMonth;
        private ImageView ivRefresh;
        private ConstraintLayout clRefreshLayout;
        private TextView tvRefreshTips;
    }

    private final ViewHolder viewHolder = new ViewHolder();

    private static class ChartData {
        private static final List<Entry> entriesWeek = new ArrayList<>();
        private static final List<String> xWeekLabels = new ArrayList<>();
        public static int weekSize;
        private static int weekTotalIncome = 0;

        private static final List<Entry> entriesMonth = new ArrayList<>();
        private static final List<String> xMonthLabels = new ArrayList<>();
        public static int monthSize;
        private static int monthTotalIncome = 0;
        private static int totalIncome = 0;
        private static final List<Entry> entriesWeekPredict = new ArrayList<>();
        private static final List<Entry> entriesMonthPredict = new ArrayList<>();
    }

    private static class HandlerWhats {
        private static final int getUserInfoHandlerWhat = 4;
        private static final int getIncomeOverallHandlerWhat = 5;

    }

    private static class UserData {
        private static Long userId;
        private static String userName;
        private static String userNickName;
        private static String userPhone;
        private static int userAvatar;
    }

    private static class Values {
        private static boolean isShowWeekData = true; // 当前显示本周/本月数据
        private static boolean isIncomeDataGot = false;
        private static boolean isAiPredictIncomeGot = false;
        private static final String TAG = "MineFragment";
        private static final Object waitIncomeDataLock = new Object();
    }

    private Context context;
    private SharedPreferencesManager sharedPreferencesManager;
    private Handler handler;
    private final MyRequest myRequest = new MyRequest();


    public MineFragment(Context context) {
        this.context = context;
    }


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

        return view;
    }

    // 初始化控件
    private void initView(View view) {
        context = view.getContext();
        sharedPreferencesManager = SharedPreferencesManager.getInstance(context);
        viewHolder.root = view.findViewById(R.id.mf_root);
        viewHolder.ivAvatar = view.findViewById(R.id.mf_iv_avatar);
        viewHolder.tvNickname = view.findViewById(R.id.mf_tv_nickname);
        viewHolder.tvRiderId = view.findViewById(R.id.mf_tv_rider_id);
        viewHolder.tvEditInfo = view.findViewById(R.id.mf_tv_edit_info);
        viewHolder.tvWeekIncome = view.findViewById(R.id.mf_tv_week_income);
        viewHolder.tvMonthIncome = view.findViewById(R.id.mf_tv_month_income);
        viewHolder.tvTotalIncome = view.findViewById(R.id.mf_tv_total_income);
        viewHolder.btnLogout = view.findViewById(R.id.mf_btn_logout);
        viewHolder.tvMyIncomeDetail = view.findViewById(R.id.mf_tv_my_income_detail);
        viewHolder.tvMySetting = view.findViewById(R.id.mf_tv_my_setting);
        viewHolder.ivRefresh = view.findViewById(R.id.mf_iv_refresh);
        RotateAnimation rotateAnimation = new RotateAnimation(
                0, 360,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        );
        rotateAnimation.setDuration(1000);
        rotateAnimation.setRepeatCount(Animation.INFINITE);
        rotateAnimation.setRepeatMode(Animation.RESTART);
        rotateAnimation.setFillAfter(true);
        viewHolder.ivRefresh.setAnimation(rotateAnimation);
        viewHolder.clRefreshLayout = view.findViewById(R.id.mf_cl_refresh_layout);
        viewHolder.tvRefreshTips = view.findViewById(R.id.mf_tv_refresh_tips);
    }

    private void initHandler() {
        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);

                // 空数据处理
                if (msg.obj == null) {
                    Log.e(Values.TAG, "JSON解析失败：响应字符串为空");
                    AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
                    builder.setTitle("错误")
                            .setMessage("请求失败, 无响应")
                            .setPositiveButton("确定", null)
                            .show();
                    return; // 终止后续解析逻辑
                }


                // 响应数据处理
                JSONObject response = null;
                switch (msg.what) {
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
                                AssetsAvatarManager avatarManager = new AssetsAvatarManager(requireActivity());
                                List<Bitmap> avatars = avatarManager.getAllAvatars();
                                viewHolder.ivAvatar.setImageBitmap(avatars.get(UserData.userAvatar));
                            } else if (response.getInt("code") == 500) {
                                ToastManager.showToast(requireActivity(), "用户信息获取错误", Toast.LENGTH_SHORT);
                            }
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                        break;
                    case HandlerWhats.getIncomeOverallHandlerWhat:
                        try {
                            response = new JSONObject((String) msg.obj);

                            if (response.getInt("code") != 200) {
                                ToastManager.showToast(requireActivity(), "收入获取错误", Toast.LENGTH_SHORT);
                            }

                            JSONObject data = response.getJSONObject("data");
                            processIncomeOverallData(data);

                            if (!Values.isAiPredictIncomeGot) {
                                requestAiIncomePredict(data);
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
        viewHolder.tvEditInfo.setOnClickListener(v -> {
            Intent intent = new Intent(requireActivity(), UserInfoActivity.class);
            startActivity(intent);
        });
        // 登出点击
        // 显示登出确认对话框
        viewHolder.btnLogout.setOnClickListener(v -> showLogoutDialog());
        // 收益详情
        // TODO: 跳转到收益详情页面
        viewHolder.tvMyIncomeDetail.setOnClickListener(v -> Toast.makeText(requireActivity(), "查看我的收益详情", Toast.LENGTH_SHORT).show());
        // 设置点击
        // TODO: 跳转到系统设置页面
        viewHolder.tvMySetting.setOnClickListener(v -> Toast.makeText(requireActivity(), "进入系统设置", Toast.LENGTH_SHORT).show());

        // 本周/本月切换
        viewHolder.tvWeek.setOnClickListener(v -> {
            if (!Values.isShowWeekData) {
                if (!Values.isIncomeDataGot) {
                    requestChartData();
                    new Thread(() -> {
                        // 等待数据获取
                        Log.i(Values.TAG, "等待收入数据获取...");
                        synchronized (Values.waitIncomeDataLock) {
                            while (!Values.isIncomeDataGot) {
                                try {
                                    Values.waitIncomeDataLock.wait();// 进入等待状态
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                    throw new RuntimeException(e);
                                }
                            }
                        }
                        // 收入数据获取完成
                        Log.i(Values.TAG, "——收入数据获取完成——");
                    }).start();
                }

                Values.isShowWeekData = true;
                updateTabStyle();
                loadChartData(ChartData.entriesWeek, ChartData.xWeekLabels);
                loadChartPredictData(ChartData.entriesWeekPredict);
            }
        });
        viewHolder.tvMonth.setOnClickListener(v -> {
            if (Values.isShowWeekData) {
                if (!Values.isIncomeDataGot) {
                    requestChartData();
                    new Thread(() -> {
                        // 等待数据获取
                        Log.i(Values.TAG, "等待收入数据获取...");
                        synchronized (Values.waitIncomeDataLock) {
                            while (!Values.isIncomeDataGot) {
                                try {
                                    Values.waitIncomeDataLock.wait();// 进入等待状态
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                    throw new RuntimeException(e);
                                }
                            }
                        }
                        // 月数据获取完成
                        Log.i(Values.TAG, "——收入数据获取完成——");
                    }).start();
                }

                Values.isShowWeekData = false;
                updateTabStyle();
                loadChartData(ChartData.entriesMonth, ChartData.xMonthLabels);
                loadChartPredictData(ChartData.entriesMonthPredict);
            }
        });

        viewHolder.clRefreshLayout.setOnClickListener(v -> {
            //TODO
            if (!Values.isIncomeDataGot){
                showRefreshTips("请求图表数据中...");
            }else if (!Values.isAiPredictIncomeGot){
                showRefreshTips("AI正在预测收入...");
            }
        });
    }

    // 更新切换按钮样式（选中/未选中）
    private void updateTabStyle() {
        if (Values.isShowWeekData) {
            viewHolder.tvWeek.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_light, null));
            viewHolder.tvWeek.setTextColor(getResources().getColor(android.R.color.white, null));
            viewHolder.tvMonth.setBackgroundColor(getResources().getColor(android.R.color.white, null));
            viewHolder.tvMonth.setTextColor(getResources().getColor(android.R.color.darker_gray, null));
        } else {
            viewHolder.tvMonth.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_light, null));
            viewHolder.tvMonth.setTextColor(getResources().getColor(android.R.color.white, null));
            viewHolder.tvWeek.setBackgroundColor(getResources().getColor(android.R.color.white, null));
            viewHolder.tvWeek.setTextColor(getResources().getColor(android.R.color.darker_gray, null));
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
        viewHolder.incomeChart.setTouchEnabled(true); // 开启交互
        viewHolder.incomeChart.setHighlightPerTapEnabled(true); // 点击触发高亮
        viewHolder.incomeChart.setDragEnabled(true); // 开启拖拽
        viewHolder.incomeChart.setScaleEnabled(false); // 关闭缩放
        viewHolder.incomeChart.setScaleXEnabled(false);
        viewHolder.incomeChart.setScaleYEnabled(false);
        viewHolder.incomeChart.setPinchZoom(false); // 关闭双指缩放
        viewHolder.incomeChart.setHorizontalScrollBarEnabled(true); // 显示横向滚动条

        // X轴配置
        XAxis xAxis = viewHolder.incomeChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(7, false);
        xAxis.setDrawGridLines(false); // 隐藏X轴网格线

        // Y轴配置
        YAxis leftYAxis = viewHolder.incomeChart.getAxisLeft();
        leftYAxis.setDrawGridLines(true);
        leftYAxis.setGridColor(getResources().getColor(android.R.color.darker_gray, null));
        // 强制设置Y轴最小值为0（核心）
        leftYAxis.setAxisMinimum(0f);

        viewHolder.incomeChart.getAxisRight().setEnabled(false); // 隐藏右侧Y轴
    }

    private void requestChartData() {
        showRefreshTips("请求图表数据中...");
        viewHolder.ivRefresh.startAnimation(viewHolder.ivRefresh.getAnimation());
        // 获取本周/本月收益数据
        String key = getString(R.string.shared_preferences_token_key);
        String token = sharedPreferencesManager.get(key);
        String weekUrl = myRequest.getBaseURL(context) + "/income/getIncomeOverall";
        myRequest.get(weekUrl,
                handler,
                HandlerWhats.getIncomeOverallHandlerWhat,
                token);
    }

    private void requestUserData() {
        String key = getString(R.string.shared_preferences_token_key);
        String getUserInfoUrl = myRequest.getBaseURL(context) + "/user/id";
        String token = sharedPreferencesManager.get(key);
        myRequest.get(getUserInfoUrl,
                handler,
                HandlerWhats.getUserInfoHandlerWhat,
                token);
    }


    // 加载图表数据（本周/本月）
    private void loadChartData(List<Entry> entries, List<String> xLabels) {
        // 构建数据集
        LineDataSet dataSet = new LineDataSet(entries, "收益");
        dataSet.setColor(getResources().getColor(android.R.color.holo_orange_dark, null)); // 折线颜色
        dataSet.setCircleColor(getResources().getColor(android.R.color.holo_orange_dark, null)); // 节点颜色
        dataSet.setCircleRadius(5f); // 节点大小
        dataSet.setDrawFilled(true); // 填充折线下方区域
        dataSet.setFillColor(getResources().getColor(android.R.color.holo_orange_light, null)); // 填充颜色
        dataSet.setFillAlpha(80); // 填充透明度
        dataSet.setLineWidth(2f); // 折线宽度
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER); // 曲线模式
        dataSet.setValueTextSize(10f);

        // 构建LineData
        List<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(dataSet);
        LineData lineData = new LineData(dataSets);

        // 设置X轴标签
        viewHolder.incomeChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(xLabels));
        // 设置数据并刷新图表
        viewHolder.incomeChart.setData(lineData);

        viewHolder.incomeChart.setVisibleXRangeMaximum(7f); // 屏幕最多显示7个点
        viewHolder.incomeChart.setVisibleXRangeMinimum(5f); // 最少显示3个点，避免过度缩小

        viewHolder.incomeChart.invalidate(); // 刷新

    }

    private void loadChartPredictData(List<Entry> entries) {
        // 虚线
        DashPathEffect dashPathEffect = new DashPathEffect(new float[]{10f, 5f}, 0f);
        // 构建数据集
        LineDataSet dataSet = new LineDataSet(entries, "预测收益");
        dataSet.setColor(getResources().getColor(android.R.color.holo_blue_light, null)); // 折线颜色
        dataSet.setCircleColor(getResources().getColor(android.R.color.holo_blue_light, null)); // 节点颜色
        dataSet.setCircleRadius(5f); // 节点大小
        dataSet.setLineWidth(2f); // 折线宽度
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER); // 曲线模式
        dataSet.setValueTextSize(10f);
        dataSet.setFormLineDashEffect(dashPathEffect);

        viewHolder.incomeChart.getLineData().addDataSet(dataSet);
        // 设置数据并刷新图表
        viewHolder.incomeChart.invalidate(); // 刷新
    }

    private void processIncomeOverallData(JSONObject data) {
        dismissRefreshTips();
        ChartData.entriesWeek.clear();
        ChartData.xWeekLabels.clear();
        ChartData.entriesMonth.clear();
        ChartData.xMonthLabels.clear();

        try {
            JSONObject weekDailyIncome = data.getJSONObject("weekIncome");
            JSONObject monthDailyIncome = data.getJSONObject("monthIncome");
            int weekTotalIncome = data.getInt("weekTotal");
            int monthTotalIncome = data.getInt("monthTotal");
            int weekSize = data.getInt("weekSize");
            int monthSize = data.getInt("monthSize");
            int allTotal = data.getInt("allTotal");

            // 保存数据
            ChartData.weekTotalIncome = weekTotalIncome;
            ChartData.monthTotalIncome = monthTotalIncome;
            ChartData.weekSize = weekSize;
            ChartData.monthSize = monthSize;
            ChartData.totalIncome = allTotal;

            // 载入周数据
            int length = weekDailyIncome.length();
            for (int i = 0; i < length; i++) {
                int amount = weekDailyIncome.getInt(String.valueOf(i + 1));
                ChartData.entriesWeek.add(new Entry(i, amount, amount));
                ChartData.xWeekLabels.add("周" + String.valueOf(i + 1));
            }
            viewHolder.tvWeekIncome.setText(String.format("¥%s", ChartData.weekTotalIncome));

            // 载入月数据
            length = monthDailyIncome.length();
            for (int i = 0; i < length; i++) {
                int amount = monthDailyIncome.getInt(String.valueOf(i + 1));
                ChartData.entriesMonth.add(new Entry(i, amount, amount));
                ChartData.xMonthLabels.add(String.valueOf(i + 1) + "日");
            }
            viewHolder.tvMonthIncome.setText(String.format("¥%s", ChartData.monthTotalIncome));

            // 载入总数据
            viewHolder.tvTotalIncome.setText(String.format("¥%s", allTotal));

            if (Values.isShowWeekData) {
                loadChartData(ChartData.entriesWeek, ChartData.xWeekLabels);
            } else {
                loadChartData(ChartData.entriesMonth, ChartData.xMonthLabels);
            }

            //  唤醒等待收入数据的线程
            synchronized (Values.waitIncomeDataLock) {
                Values.isIncomeDataGot = true;
                Values.waitIncomeDataLock.notify();
            }

            if (Values.isAiPredictIncomeGot && Values.isIncomeDataGot) {
                viewHolder.ivRefresh.getAnimation().cancel();
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

    }

    private void processIncomeAiPredictData(JSONObject data) {
        ChartData.entriesWeekPredict.clear();
        ChartData.entriesMonthPredict.clear();
        try {
            JSONObject weekDailyIncome = data.getJSONObject("weekIncome");
            JSONObject monthDailyIncome = data.getJSONObject("monthIncome");

            // 载入周预测数据
            int length = weekDailyIncome.length();
            for (int i = ChartData.weekSize; i < length; i++) {   // 只保留预测数据
                int amount = weekDailyIncome.getInt(String.valueOf(i + 1));
                ChartData.entriesWeekPredict.add(new Entry(i, amount, amount));
            }
            // 载入月数据
            length = monthDailyIncome.length();
            for (int i = ChartData.monthSize; i < length; i++) {  // 只保留预测数据
                int amount = monthDailyIncome.getInt(String.valueOf(i + 1));
                ChartData.entriesMonthPredict.add(new Entry(i, amount, amount));
            }

            if (Values.isShowWeekData) {
                loadChartPredictData(ChartData.entriesWeekPredict);
            } else {
                loadChartPredictData(ChartData.entriesMonthPredict);
            }

            if (Values.isAiPredictIncomeGot && Values.isIncomeDataGot) {
                viewHolder.ivRefresh.getAnimation().cancel();
                dismissRefreshTips();
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private void requestAiIncomePredict(JSONObject dataOrigin) {
        if (Values.isAiPredictIncomeGot) {
            return;
        }
        showRefreshTips("AI正在预测收入...");
        viewHolder.ivRefresh.startAnimation(viewHolder.ivRefresh.getAnimation());


        // 获取当前时间
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String currentTime = now.format(formatter);

        StringBuilder resultBuilder = new StringBuilder();
        String userPrompt = dataOrigin.toString();
        String systemPrompt = getString(R.string.AI_SYSTEM_PROMPT_INCOME_PREDICT)
                + "[辅助数据]   当前时间：" + currentTime;
        AIApiClient.getInstance().callChatApi(userPrompt, systemPrompt, new AIApiClient.AiChatCallback() {
            @Override
            public void onMessageReceived(String content) {
                resultBuilder.append(content);
                Log.d("AI_RESULT", "收到内容：" + content);
            }

            @Override
            public void onComplete() {
                // 请求完成，可做后续处理（如解析JSON）
                Log.d("AI_RESULT", "请求完成，完整结果：" + resultBuilder.toString());
                try {
                    JSONObject result = new JSONObject(resultBuilder.toString());
                    Values.isAiPredictIncomeGot = true;
                    processIncomeAiPredictData(result);
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void onError(String errorMsg) {
                // 处理错误
                Log.e("AI_ERROR", errorMsg);
            }
        });
    }

    // 退出登录对话框
    private void showLogoutDialog() {
        new AlertDialog.Builder(requireActivity())
                .setTitle("退出登录")
                .setMessage("确定要退出当前账号吗？")
                .setPositiveButton("确定", (dialog, which) -> {
                    ToastManager.showToast(context, "已退出登录", Toast.LENGTH_SHORT);
                    //TODO 退出登录逻辑
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

        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); // 设置背景透明

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
            ToastManager.showToast(context, "头像更换成功", Toast.LENGTH_SHORT);
            // 关闭弹窗
            dialog.dismiss();
        });

        // 显示弹窗
        dialog.show();
    }

    private void showRefreshTips(String text) {
        viewHolder.tvRefreshTips.setText(text);
        viewHolder.tvRefreshTips.setVisibility(View.VISIBLE);
    }

    private void dismissRefreshTips() {
        viewHolder.tvRefreshTips.setVisibility(View.GONE);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        myRequest.closeStream();
    }
}