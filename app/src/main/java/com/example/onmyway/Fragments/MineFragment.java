package com.example.onmyway.Fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
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

import com.example.onmyway.Activities.MainActivity;
import com.example.onmyway.Adapter.AvatarSelectAdapter;
import com.example.onmyway.Entity.Avatar;
import com.example.onmyway.R;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import java.util.ArrayList;
import java.util.List;

public class MineFragment extends Fragment {

    private Context context;

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
    private boolean isWeekData = true; // 当前显示本周/本月数据

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mine, container, false);

        // 初始化控件
        initView(view);
        // 初始化图表控件
        initChartView(view);

        // 设置点击事件
        setClickEvents();

        // 加载用户数据
        loadUserData();
        // 初始化并绘制图表（默认显示本周数据）
        initChart();
        loadChartData(isWeekData);

        return view;
    }

    // 初始化控件
    private void initView(View view) {
        context = view.getContext();
        ivAvatar = view.findViewById(R.id.mf_iv_avatar);
        tvNickname = view.findViewById(R.id.mf_tv_nickname);
        tvRiderId = view.findViewById(R.id.mf_tv_rider_id);
        tvEditInfo = view.findViewById(R.id.mf_tv_edit_info);
        tvTodayIncome = view.findViewById(R.id.mf_tv_today_income);
        tvMonthIncome = view.findViewById(R.id.mf_tv_month_income);
        tvTotalIncome = view.findViewById(R.id.mf_tv_total_income);
        btnLogout = view.findViewById(R.id.mf_btn_logout);
        tvMyIncomeDetail = view.findViewById(R.id.mf_tv_my_income_detail);
        tvMySetting = view.findViewById(R.id.mf_tv_my_setting);
    }

    // 设置点击事件
    private void setClickEvents() {

        // 头像点击
        // TODO: 跳转到修改头像页面
        ivAvatar.setOnClickListener(v -> {
            showAvatarSelectDialog();
        });
        // 修改信息点击
        // TODO: 跳转到修改信息页面
        tvEditInfo.setOnClickListener(v -> Toast.makeText(getActivity(), "进入修改信息页面", Toast.LENGTH_SHORT).show());
        // 登出点击
        // 显示登出确认对话框
        btnLogout.setOnClickListener(v -> showLogoutDialog());
        // 收益详情
        // TODO: 跳转到收益详情页面
        tvMyIncomeDetail.setOnClickListener(v -> Toast.makeText(getActivity(), "查看我的收益详情", Toast.LENGTH_SHORT).show());
        // 设置点击
        // TODO: 跳转到系统设置页面
        tvMySetting.setOnClickListener(v -> Toast.makeText(getActivity(), "进入系统设置", Toast.LENGTH_SHORT).show());

        // 本周/本月切换
        tvWeek.setOnClickListener(v -> {
            if (!isWeekData) {
                isWeekData = true;
                updateTabStyle();
                loadChartData(isWeekData);
            }
        });
        tvMonth.setOnClickListener(v -> {
            if (isWeekData) {
                isWeekData = false;
                updateTabStyle();
                loadChartData(isWeekData);
            }
        });
    }

    // 更新切换按钮样式（选中/未选中）
    private void updateTabStyle() {
        if (isWeekData) {
            tvWeek.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_light));
            tvWeek.setTextColor(getResources().getColor(android.R.color.white));
            tvMonth.setBackgroundColor(getResources().getColor(android.R.color.white));
            tvMonth.setTextColor(getResources().getColor(android.R.color.darker_gray));
        } else {
            tvMonth.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_light));
            tvMonth.setTextColor(getResources().getColor(android.R.color.white));
            tvWeek.setBackgroundColor(getResources().getColor(android.R.color.white));
            tvWeek.setTextColor(getResources().getColor(android.R.color.darker_gray));
        }
    }

    // 初始化图表控件
    private void initChartView(View view) {
        incomeChart = view.findViewById(R.id.mf_income_chart);
        tvWeek = view.findViewById(R.id.mf_tv_week);
        tvMonth = view.findViewById(R.id.mf_tv_month);
    }

    // 初始化图表样式
    private void initChart() {
        // 隐藏描述文字
        Description description = new Description();
        description.setEnabled(false);
        incomeChart.setDescription(description);

        // 隐藏图例
        incomeChart.getLegend().setEnabled(false);

        // 禁用触摸缩放
        incomeChart.setScaleEnabled(false);

        // X轴配置
        XAxis xAxis = incomeChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false); // 隐藏X轴网格线

        // Y轴配置
        YAxis leftYAxis = incomeChart.getAxisLeft();
        leftYAxis.setDrawGridLines(true);
        leftYAxis.setGridColor(getResources().getColor(android.R.color.darker_gray));
        incomeChart.getAxisRight().setEnabled(false); // 隐藏右侧Y轴

        // 禁用图表点击
        incomeChart.setClickable(false);
    }

    // 加载图表数据（本周/本月）
    private void loadChartData(boolean isWeek) {
        List<Entry> entries = new ArrayList<>();
        String[] xLabels; // X轴标签

        if (isWeek) {
            // 本周数据（7天）
            xLabels = new String[]{"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
            // 模拟本周收益数据（x轴索引，y轴收益）
            entries.add(new Entry(0, 89.5f));
            entries.add(new Entry(1, 120.8f));
            entries.add(new Entry(2, 98.2f));
            entries.add(new Entry(3, 156.5f));
            entries.add(new Entry(4, 110.3f));
            entries.add(new Entry(5, 135.7f));
            entries.add(new Entry(6, 95.6f));
        } else {
            // 本月数据（取前10天示例）
            xLabels = new String[]{"1日", "2日", "3日", "4日", "5日", "6日", "7日", "8日", "9日", "10日"};
            // 模拟本月收益数据
            entries.add(new Entry(0, 89.5f));
            entries.add(new Entry(1, 120.8f));
            entries.add(new Entry(2, 98.2f));
            entries.add(new Entry(3, 156.5f));
            entries.add(new Entry(4, 110.3f));
            entries.add(new Entry(5, 135.7f));
            entries.add(new Entry(6, 95.6f));
            entries.add(new Entry(7, 140.2f));
            entries.add(new Entry(8, 105.8f));
            entries.add(new Entry(9, 128.5f));
        }

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
        incomeChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(xLabels));

        // 设置数据并刷新图表
        incomeChart.setData(lineData);
        incomeChart.invalidate(); // 刷新
    }

    // 加载用户数据
    private void loadUserData() {
        tvNickname.setText("贤");
        tvRiderId.setText("骑手ID：11111");
        tvTodayIncome.setText("¥1000");
        tvMonthIncome.setText("¥20000");
        tvTotalIncome.setText("¥30000");
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
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_avatar_select, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); // 设置背景透明

        //TODO 优化头像读取
        // 获取头像资源ID
        int[] AVATAR_RES_IDS = {
                R.drawable.avatar_0,
                R.drawable.avatar_1,
                R.drawable.avatar_2,
                R.drawable.avatar_3,
                R.drawable.avatar_4
        };

        // 初始化头像列表数据
        List<Avatar> avatarList = new ArrayList<>();
        for (int resId : AVATAR_RES_IDS) {
            avatarList.add(new Avatar(resId, false));
        }

        // 初始化GridView和适配器
        GridView gvAvatar = dialogView.findViewById(R.id.gv_avatar_list);
        AvatarSelectAdapter adapter = new AvatarSelectAdapter(context, avatarList);
        gvAvatar.setAdapter(adapter);

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
            ivAvatar.setImageResource(AVATAR_RES_IDS[selectedPos]);
            // 提示更换成功
            Toast.makeText(context, "头像更换成功", Toast.LENGTH_SHORT).show();
            // 关闭弹窗
            dialog.dismiss();
        });

        // 显示弹窗
        dialog.show();
    }
}