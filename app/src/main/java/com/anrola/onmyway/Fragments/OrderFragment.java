package com.anrola.onmyway.Fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Toast;

import com.anrola.onmyway.Activities.MainActivity;
import com.anrola.onmyway.Adapter.OrderListAdapter;
import com.anrola.onmyway.Entity.Order;
import com.anrola.onmyway.R;
import com.anrola.onmyway.Utils.MyRequest;
import com.anrola.onmyway.Utils.SharedPreferencesManager;
import com.google.android.material.tabs.TabLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class OrderFragment extends Fragment {
    private Context context;
    private final MyRequest myRequest = new MyRequest();
    private Handler handler;
    private SharedPreferencesManager sharedPreferencesManager;


    private List<Order> unacceptedOrderList = new ArrayList<>();
    private List<Order> acceptedOrderList = new ArrayList<>();
    private OrderListAdapter orderListAdapter;


    private static class ViewHolder {
        private RecyclerView rvOrderList;
        private TabLayout tabOrderType;
        private Spinner spinnerSort;
        private Button btnOrderSetting;
        private Button btnOrderRefresh;
        private SwitchCompat swAutoAccept;
    }

    ViewHolder viewHolder = new ViewHolder();

    private static class Values {

        private static final String TAG = "OrderFragment";
        private static String baseURL;
        private static boolean isUnreceivedOrderPage = true;
        private static boolean isReceivedOrderGot = false;
        private static final Object waitReceivedOrderGotLock = new Object();
        private static final Object waitAutoAcceptReStartLock = new Object();

        private static int sortIndex = 0;
        private static boolean isAutoAccept = false;

        private static int autoAcceptMax = 1;
    }

    private static class HandlerWhats {
        private static final int getAndUpdateUnAcceptedOrderHandlerWhat = 1;
        private static final int getAndUpdateAcceptedOrderHandlerWhat = 2;
        private static final int acceptOrderHandlerWhat = 3;
        private static final int pickUpOrderHandlerWhat = 4;
        private static final int getAcceptedOrderHandlerWhat = 5;
        private static final int doneOrderHandlerWhat = 6;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_order, container, false);

        initViews(view);
        initHandler();
        initValues();

        requestAndUpdateOrder(Values.isUnreceivedOrderPage, Values.sortIndex);
        setClickListener();
        initRecyclerView();

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }

    private void initViews(View view) {
        context = view.getContext();
        sharedPreferencesManager = SharedPreferencesManager.getInstance(context);
        Values.baseURL = myRequest.getBaseURL(context);
        viewHolder.rvOrderList = view.findViewById(R.id.of_rv_order_list);
        viewHolder.tabOrderType = view.findViewById(R.id.of_tab_order_type);
        viewHolder.spinnerSort = view.findViewById(R.id.of_spinner_sort);
        viewHolder.btnOrderSetting = view.findViewById(R.id.of_btn_order_setting);
        viewHolder.swAutoAccept = view.findViewById(R.id.of_switch_auto_accept);
    }

    private void initValues() {
    }

    private void initRecyclerView() {
        // 设置布局管理器
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setOrientation(RecyclerView.VERTICAL);
        viewHolder.rvOrderList.setLayoutManager(linearLayoutManager);

        // 初始化适配器并绑定
        orderListAdapter = new OrderListAdapter(getContext(), unacceptedOrderList);
        viewHolder.rvOrderList.setAdapter(orderListAdapter);

        // 设置接单按钮点击事件
        orderListAdapter.setOnTakeOrderClickListener((position, order) -> {
            requestAcceptOrder(order.getNo());
        });
        // 设置取货按钮点击事件
        orderListAdapter.setOnPickOrderClickListener((position, order) -> {
            requestPickUpOrder(order.getNo());
        });
        // 订单完成按钮点击事件
        orderListAdapter.setOnDoneOrderClickListener((position, order) -> {
            requestFinishOrder(order.getNo());
        });
    }

    private void initHandler() {
        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);

                // 空数据处理
                if (msg.obj == null) {
                    Log.e(Values.TAG, "JSON解析失败：响应字符串为空");
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle("错误")
                            .setMessage("请求失败, 无响应")
                            .setPositiveButton("确定", null)
                            .show();
                    return; // 终止后续解析逻辑
                }


                // 响应数据处理
                switch (msg.what) {
                    case HandlerWhats.getAndUpdateUnAcceptedOrderHandlerWhat: // 获取并更新未接单订单
                        try {
                            JSONArray jsonArray = new JSONArray(msg.obj.toString());
                            unacceptedOrderList = getOrderListByJSONArray(jsonArray);
                            orderListAdapter.updateData(unacceptedOrderList);
                        } catch (JSONException e) {
                            Log.e(Values.TAG, "JSON解析失败：" + e.getMessage());
                        }
                        break;
                    case HandlerWhats.getAndUpdateAcceptedOrderHandlerWhat: // 获取并更新已接单订单
                        try {
                            JSONArray jsonArray = new JSONArray(msg.obj.toString());
                            acceptedOrderList = getOrderListByJSONArray(jsonArray);
                            orderListAdapter.updateData(acceptedOrderList);


                            Values.isReceivedOrderGot = true;

                            MainActivity mainActivity = (MainActivity) requireActivity();
                            NavFragment navFragment = (NavFragment) mainActivity.getNavFragment();
                            navFragment.setAcceptedOrders(acceptedOrderList);
                            navFragment.notifyDeliveryPlanLock();
                        } catch (JSONException e) {
                            Log.e(Values.TAG, "JSON解析失败：" + e.getMessage());
                        }
                        break;
                    case HandlerWhats.acceptOrderHandlerWhat: // 接单
                        try {
                            JSONObject jsonObject = new JSONObject(msg.obj.toString());
                            String orderNo = jsonObject.getString("data");
                            Toast.makeText(getContext(), jsonObject.getString("msg"), Toast.LENGTH_SHORT).show();

                            int position = orderListAdapter.getPositionByOderNo(orderNo);
                            orderListAdapter.removeOrder(position);
                            acceptedOrderList.add(orderListAdapter.getOrderByOderNo(orderNo));

                        } catch (JSONException e) {
                            Log.e(Values.TAG, "JSON解析失败：" + e.getMessage());
                        }
                        break;
                    case HandlerWhats.pickUpOrderHandlerWhat:   // 取货
                        try {
                            JSONObject jsonObject = new JSONObject(msg.obj.toString());
                            String orderNo = jsonObject.getString("data");
                            Toast.makeText(getContext(), jsonObject.getString("msg"), Toast.LENGTH_SHORT).show();
                            orderListAdapter.updateOrderPicked(orderNo, true);
                        } catch (JSONException e) {
                            Log.e(Values.TAG, "JSON解析失败：" + e.getMessage());
                        }
                        break;
                    case HandlerWhats.getAcceptedOrderHandlerWhat:  // 获取已接单订单
                        try {
                            JSONArray jsonArray = new JSONArray(msg.obj.toString());
                            acceptedOrderList = getOrderListByJSONArray(jsonArray);
                            Values.isReceivedOrderGot = true;
                            // 唤醒
                            synchronized (Values.waitReceivedOrderGotLock) {
                                Values.waitReceivedOrderGotLock.notifyAll();
                            }
                        } catch (JSONException e) {
                            Log.e(Values.TAG, "JSON解析失败：" + e.getMessage());
                        }
                        break;
                    case HandlerWhats.doneOrderHandlerWhat: // 订单完成
                        try {
                            JSONObject jsonObject = new JSONObject(msg.obj.toString());
                            String orderNo = jsonObject.getString("data");
                            Toast.makeText(getContext(), jsonObject.getString("msg"), Toast.LENGTH_SHORT).show();
                            orderListAdapter.updateOrderFinished(orderNo, true);
                            orderListAdapter.getPositionByOderNo(orderNo);
                            orderListAdapter.removeOrder(orderListAdapter.getPositionByOderNo(orderNo));
                        } catch (JSONException e) {
                            Log.e(Values.TAG, "JSON解析失败：" + e.getMessage());
                        }
                        break;
                }
            }
        };
    }
    private void setClickListener() {
        viewHolder.tabOrderType.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0:
                        Values.isUnreceivedOrderPage = true;
                        break;
                    case 1:
                        Values.isUnreceivedOrderPage = false;
                        break;
                }
                requestAndUpdateOrder(Values.isUnreceivedOrderPage, Values.sortIndex);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
        viewHolder.spinnerSort.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Values.sortIndex = position;
                unacceptedOrderList.clear();
                requestAndUpdateOrder(Values.isUnreceivedOrderPage, Values.sortIndex);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        viewHolder.btnOrderSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showOrderSettingDialog();

            }
        });
        viewHolder.swAutoAccept.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    startAutoAcceptOrder();
                } else {
                    stopAutoAcceptOrder();
                }
            }
        });
    }
    private void showOrderSettingDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(null);
        builder.setPositiveButton(null, null);
        builder.setNegativeButton(null, null);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.fo_dialog_order_setting, null);

        Button btnSaveSetting = dialogView.findViewById(R.id.btn_save_setting);
        Spinner spinnerAmount = dialogView.findViewById(R.id.fo_spinner_amount);
        Spinner spinnerDistance = dialogView.findViewById(R.id.fo_spinner_distance);
        Spinner spinnerAutoAcceptMax = dialogView.findViewById(R.id.fo_spinner_auto_accept_max);
        Spinner spinnerAutoAcceptPriority = dialogView.findViewById(R.id.fo_spinner_auto_accept_priority);


        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        dialog.setCancelable(false); // 禁止返回键取消
        dialog.setCanceledOnTouchOutside(false); // 禁止点击空白处取消


        btnSaveSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO: 保存设置

                Values.autoAcceptMax = Integer.parseInt(spinnerAutoAcceptMax.getSelectedItem().toString());

                dialog.dismiss();
            }
        });

        dialog.show();
    }

    public void requestAndUpdateOrder(boolean isUnreceivedOrderPage, int orderBy) {
        if (isUnreceivedOrderPage) {
            String url = Values.baseURL + "/order/all/unaccepted?" + "orderBy=" + orderBy;
            myRequest.get(url, handler, HandlerWhats.getAndUpdateUnAcceptedOrderHandlerWhat);
        } else {
            String url = Values.baseURL + "/order/all/accepted?" + "orderBy=" + orderBy;
            String key = getString(R.string.shared_preferences_token_key);
            String token = sharedPreferencesManager.get(key);
            myRequest.get(url, handler, HandlerWhats.getAndUpdateAcceptedOrderHandlerWhat, token);
        }
    }

    public void requestAndUpdateAcceptOrderInNavFragment() {
        viewHolder.tabOrderType.selectTab(viewHolder.tabOrderType.getTabAt(1));
    }

    private void requestAcceptedOrder(int orderBy) {
        String url = Values.baseURL + "/order/all/accepted?" + "orderBy=" + orderBy;
        String key = getString(R.string.shared_preferences_token_key);
        String token = sharedPreferencesManager.get(key);
        myRequest.get(url, handler, HandlerWhats.getAcceptedOrderHandlerWhat, token);
    }

    private void requestAcceptOrder(String orderNo) {
        String url = Values.baseURL + "/order/accept?orderNo=" + orderNo;
        String key = getString(R.string.shared_preferences_token_key);
        String token = sharedPreferencesManager.get(key);
        myRequest.get(url, handler, HandlerWhats.acceptOrderHandlerWhat, token);
    }

    private void requestPickUpOrder(String orderNo) {
        String url = Values.baseURL + "/order/pickup?orderNo=" + orderNo;
        String key = getString(R.string.shared_preferences_token_key);
        String token = sharedPreferencesManager.get(key);
        myRequest.get(url, handler, HandlerWhats.pickUpOrderHandlerWhat, token);

    }

    private void requestFinishOrder(String orderNo) {
        String url = Values.baseURL + "/order/finish?orderNo=" + orderNo;
        String key = getString(R.string.shared_preferences_token_key);
        String token = sharedPreferencesManager.get(key);
        myRequest.get(url, handler, HandlerWhats.doneOrderHandlerWhat, token);
    }

    private List<Order> getOrderListByJSONArray(JSONArray jsonArray) {
        List<Order> orderList = new ArrayList<>();
        try {
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                Order order = new Order(
                        jsonObject.getString("no"),
                        jsonObject.getString("title"),
                        jsonObject.getString("receiverName"),
                        jsonObject.getString("receiverMobile"),
                        jsonObject.getString("receiverProvince"),
                        jsonObject.getString("receiverCity"),
                        jsonObject.getString("receiverDistrict"),
                        jsonObject.getString("receiverAddress"),
                        jsonObject.getJSONObject("startLocation"),
                        jsonObject.getJSONObject("endLocation"),
                        jsonObject.getInt("distance"),
                        jsonObject.getInt("amount"),
                        jsonObject.getString("startTime"),
                        jsonObject.getString("requireTime"),
                        jsonObject.getBoolean("isAccepted"),
                        jsonObject.getBoolean("isPickUp"),
                        jsonObject.getBoolean("isFinished")
                );
                orderList.add(order);
            }
            return orderList;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private void startAutoAcceptOrder() {
        Values.isAutoAccept = true;


        new Thread(new Runnable() {
            @Override
            public void run() {


                if (!Values.isReceivedOrderGot) {
                    // 等待请求接收订单
                    Log.i(Values.TAG, "等待请求已接单...");

                    requestAcceptedOrder(Values.sortIndex);

                    synchronized (Values.waitReceivedOrderGotLock) {
                        while (!Values.isReceivedOrderGot) {
                            try {
                                Values.waitReceivedOrderGotLock.wait();
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }
                        Log.i(Values.TAG, "——已接单订单已获取——");
                    }
                }


                // 开始自动接单
                Log.i(Values.TAG, "开始自动接单...");
                while (Values.isAutoAccept) {

                    int size = acceptedOrderList.size();
                    if (size >= Values.autoAcceptMax) {
                        Log.i(Values.TAG, "到达最大接单数，暂停自动接单");
                        requireActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getActivity(), "到达最大接单数，停止自动接单", Toast.LENGTH_SHORT).show();
                            }
                        });

                        // 等待自动接单重新开始
                        Values.isAutoAccept  = false;
                        synchronized (Values.waitAutoAcceptReStartLock){
                            while (!Values.isAutoAccept) {
                                try {
                                    Values.waitAutoAcceptReStartLock.wait();
                                } catch (InterruptedException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }

                    } else {
                        // 自动接单

                        List<Order> acceptedOrderList_sort = new ArrayList<>(unacceptedOrderList);
                        acceptedOrderList_sort.sort(Comparator.comparingInt(Order::getAmount).reversed());

                        if (acceptedOrderList_sort.isEmpty()) {
                            Log.i(Values.TAG, "无可接取订单");
                            return;
                        } else { // 有可接取订单
                            int position = orderListAdapter.getPositionByOderNo(acceptedOrderList_sort.get(0).getNo());
                            requireActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Log.i(Values.TAG, "自动接取订单："+acceptedOrderList_sort.get(0).getTitle());
                                    acceptOrderByPosition(position);
                                }
                            });
                        }
                    }

                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                Log.i(Values.TAG, "自动接单已停止");
            }
        }).start();
    }

    public void acceptOrderByPosition(int position) {
        RecyclerView.ViewHolder rvViewHolder = viewHolder.rvOrderList.findViewHolderForAdapterPosition(position);


        if (rvViewHolder == null) {
            viewHolder.rvOrderList.post(() -> {
                RecyclerView.ViewHolder rvViewHolder_retry = viewHolder.rvOrderList.findViewHolderForAdapterPosition(0);
                if (rvViewHolder_retry != null) {
                    rvViewHolder_retry.itemView.findViewById(R.id.btn_take_order).performClick();   // 点击接单按钮
                }
            });
            return;
        }

        rvViewHolder.itemView.findViewById(R.id.btn_take_order).performClick(); // 点击接单按钮
    }

    private void stopAutoAcceptOrder() {
        Values.isAutoAccept = false;
    }
}