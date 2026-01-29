package com.anrola.onmyway.Fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
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
import java.util.List;

public class OrderFragment extends Fragment {
    private Context context;
    private final MyRequest myRequest = new MyRequest();
    private Handler handler;
    private SharedPreferencesManager sharedPreferencesManager;


    private List<Order> unacceptOrderList = new ArrayList<>();
    private List<Order> acceptedOrderList = new ArrayList<>();
    private OrderListAdapter orderListAdapter;


    private static class ViewHolder {
        private RecyclerView vpOrderList;
        private TabLayout tabOrderType;
        private Spinner spinnerSort;
        private Button btnOrderSetting;
        private Button btnOrderRefresh;
    }
    private final ViewHolder viewHolder = new ViewHolder();

    private static class Value {

        private static final String TAG = "OrderFragment";
        private static String baseURL;
        private static boolean isUnreceivedOrderPage = true;
        private static int orderByIndex = 0;
    }

    private static class HandlerWhats {
        private static final int getUnAcceptedOrderHandlerWhat = 1;
        private static final int getAcceptedOrderHandlerWhat = 2;
        private static final int acceptOrderHandlerWhat = 3;
        private static final int pickUpOrderHandlerWhat = 4;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_order, container, false);

        initViews(view);
        initHandler();
        initValues();

        requestOrder(Value.isUnreceivedOrderPage, 0);
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
        Value.baseURL = myRequest.getBaseURL(context);
        viewHolder.vpOrderList = view.findViewById(R.id.of_vp_order_list);
        viewHolder.tabOrderType = view.findViewById(R.id.of_tab_order_type);
        viewHolder.spinnerSort = view.findViewById(R.id.of_spinner_sort);
        viewHolder.btnOrderSetting = view.findViewById(R.id.of_btn_order_setting);
    }

    private void initValues() {
    }

    private void initRecyclerView() {
        // 设置布局管理器
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setOrientation(RecyclerView.VERTICAL);
        viewHolder.vpOrderList.setLayoutManager(linearLayoutManager);

        // 初始化适配器并绑定
        orderListAdapter = new OrderListAdapter(getContext(), unacceptOrderList);
        viewHolder.vpOrderList.setAdapter(orderListAdapter);

        // 设置接单按钮点击事件
        orderListAdapter.setOnTakeOrderClickListener((position, order) -> {
            requestAcceptOrder(order.getNo());
        });
        // 设置取货按钮点击事件
        orderListAdapter.setOnPickOrderClickListener((position, order) -> {
            pickUpOrder(order.getNo());
        });
    }

    private void initHandler() {
        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);

                // 空数据处理
                if (msg.obj == null) {
                    Log.e(Value.TAG, "JSON解析失败：响应字符串为空");
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle("错误")
                            .setMessage("请求失败, 无响应")
                            .setPositiveButton("确定", null)
                            .show();
                    return; // 终止后续解析逻辑
                }


                // 响应数据处理
                switch (msg.what) {
                    case HandlerWhats.getUnAcceptedOrderHandlerWhat: // 获取未接单订单
                        try {
                            JSONArray jsonArray = new JSONArray(msg.obj.toString());
                            unacceptOrderList = getOrderListByJSONArray(jsonArray);
                            orderListAdapter.updateData(unacceptOrderList);
                        } catch (JSONException e) {
                            Log.e(Value.TAG, "JSON解析失败：" + e.getMessage());
                        }
                        break;
                    case HandlerWhats.getAcceptedOrderHandlerWhat: // 获取已接单订单
                        try {
                            JSONArray jsonArray = new JSONArray(msg.obj.toString());
                            acceptedOrderList = getOrderListByJSONArray(jsonArray);
                            orderListAdapter.updateData(acceptedOrderList);


                            MainActivity mainActivity = (MainActivity) requireActivity();
                            NavFragment navFragment = (NavFragment) mainActivity.getNavFragment();
                            navFragment.setAcceptedOrders(acceptedOrderList);
                            navFragment.notifyDeliveryPlanLock();
                        } catch (JSONException e) {
                            Log.e(Value.TAG, "JSON解析失败：" + e.getMessage());
                        }
                        break;
                    case HandlerWhats.acceptOrderHandlerWhat: // 接单
                        try {
                            JSONObject jsonObject = new JSONObject(msg.obj.toString());
                            String orderNo = jsonObject.getString("data");
                            Toast.makeText(getContext(), jsonObject.getString("msg"), Toast.LENGTH_SHORT).show();

                            int position = orderListAdapter.getPositionByOderNo(orderNo);
                            orderListAdapter.removeOrder(position);
                        } catch (JSONException e) {
                            Log.e(Value.TAG, "JSON解析失败：" + e.getMessage());
                        }
                        break;
                    case HandlerWhats.pickUpOrderHandlerWhat:
                        try {
                            JSONObject jsonObject = new JSONObject(msg.obj.toString());
                            String orderNo = jsonObject.getString("data");
                            Toast.makeText(getContext(), jsonObject.getString("msg"), Toast.LENGTH_SHORT).show();
                            orderListAdapter.updateOrderPicked(orderNo, true);
                        } catch (JSONException e) {
                            Log.e(Value.TAG, "JSON解析失败：" + e.getMessage());
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
                        Value.isUnreceivedOrderPage = true;
                        break;
                    case 1:
                        Value.isUnreceivedOrderPage = false;
                        break;
                }
                unacceptOrderList.clear();
                acceptedOrderList.clear();
                requestOrder(Value.isUnreceivedOrderPage, Value.orderByIndex);
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
                Value.orderByIndex = position;
                unacceptOrderList.clear();
                requestOrder(Value.isUnreceivedOrderPage, Value.orderByIndex);
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
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    public void requestOrder(boolean isUnreceivedOrderPage, int orderBy) {
        if (isUnreceivedOrderPage) {
            String url = Value.baseURL + "/order/all/unaccepted?" + "orderBy=" + orderBy;
            myRequest.get(url, handler, HandlerWhats.getUnAcceptedOrderHandlerWhat);
        } else {
            String url = Value.baseURL + "/order/all/accepted?" + "orderBy=" + orderBy;
            String key = getString(R.string.shared_preferences_token_key);
            String token = sharedPreferencesManager.get(key);
            myRequest.get(url, handler, HandlerWhats.getAcceptedOrderHandlerWhat, token);
        }
    }

    public void requestAcceptOrderInNavFragment() {
        viewHolder.tabOrderType.selectTab(viewHolder.tabOrderType.getTabAt(1));
    }

    private void requestAcceptOrder(String orderNo) {
        String url = Value.baseURL + "/order/accept?orderNo=" + orderNo;
        String key = getString(R.string.shared_preferences_token_key);
        String token = sharedPreferencesManager.get(key);
        myRequest.get(url, handler, HandlerWhats.acceptOrderHandlerWhat, token);
    }

    private void pickUpOrder(String orderNo) {

        String url = Value.baseURL + "/order/pickup?orderNo=" + orderNo;
        String key = getString(R.string.shared_preferences_token_key);
        String token = sharedPreferencesManager.get(key);
        myRequest.get(url, handler, HandlerWhats.pickUpOrderHandlerWhat, token);

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
}