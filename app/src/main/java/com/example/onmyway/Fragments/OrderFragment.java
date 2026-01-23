package com.example.onmyway.Fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
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
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.Toast;

import com.example.onmyway.Adapter.OrderListAdapter;
import com.example.onmyway.Entity.Order;
import com.example.onmyway.R;
import com.example.onmyway.Utils.MyRequest;
import com.github.mikephil.charting.data.Entry;
import com.google.android.material.tabs.TabItem;
import com.google.android.material.tabs.TabLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OrderFragment extends Fragment {
    private Context context;
    private MyRequest myRequest = new MyRequest();
    private Handler handler;
    private SharedPreferences sharedPreferences;

    private List<Order> orderList = new ArrayList<>();
    private OrderListAdapter orderListAdapter;


    private static class ViewHolder {
        private static RecyclerView vpOrderList;
        private static TabLayout tabOrderType;
        private static Spinner spinnerSort;
    }

    private static class Value {

        private static final String TAG = "OrderFragment";
        private static String baseURL;
        private static boolean isUnreceivedOrderPage = true;
        private static int orderByIndex = 0;
    }

    private static class HandlerWhats {
        private static final int getOrderHandlerWhat = 1;
        private static final int acceptOrderHandlerWhat = 2;
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

    private void initViews(View view) {
        context = view.getContext();
        Value.baseURL = myRequest.getBaseURL(context);
        sharedPreferences = requireActivity().getSharedPreferences(
                getString(R.string.shared_preferences_user_info_key),
                Context.MODE_PRIVATE);
        ViewHolder.vpOrderList = view.findViewById(R.id.of_vp_order_list);
        ViewHolder.tabOrderType = view.findViewById(R.id.of_tab_order_type);
        ViewHolder.spinnerSort = view.findViewById(R.id.of_spinner_sort);
    }

    private void initValues() {
    }

    private void initRecyclerView() {
        // 设置布局管理器
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setOrientation(RecyclerView.VERTICAL);
        ViewHolder.vpOrderList.setLayoutManager(linearLayoutManager);

        // 初始化适配器并绑定
        orderListAdapter = new OrderListAdapter(getContext(), orderList);
        ViewHolder.vpOrderList.setAdapter(orderListAdapter);

        // 设置接单按钮点击事件
        orderListAdapter.setOnTakeOrderClickListener((position, order) -> {
            requestAcceptOrder(order.getNo());
        });
    }

    private void initHandler() {
        //TODO
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
                    case HandlerWhats.getOrderHandlerWhat: // 获取未接单订单
                        try {
                            JSONArray jsonArray = new JSONArray(msg.obj.toString());
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject jsonObject = jsonArray.getJSONObject(i);
                                Order order = new Order();
                                order.setNo(jsonObject.getString("no"));
                                order.setTitle(jsonObject.getString("title"));
                                order.setReceiverName(jsonObject.getString("receiverName"));
                                order.setReceiverMobile(jsonObject.getString("receiverMobile"));
                                order.setReceiverProvince(jsonObject.getString("receiverProvince"));
                                order.setReceiverCity(jsonObject.getString("receiverCity"));
                                order.setReceiverDistrict(jsonObject.getString("receiverDistrict"));
                                order.setReceiverAddress(jsonObject.getString("receiverAddress"));
                                order.setStartLocation(jsonObject.getString("startLocation"));
                                order.setEndLocation(jsonObject.getString("endLocation"));
                                order.setDistance(jsonObject.getInt("distance"));
                                order.setAmount(jsonObject.getInt("amount"));
                                order.setStartTime(jsonObject.getString("startTime"));
                                order.setRequireTime(jsonObject.getString("requireTime"));
                                order.setStatus(jsonObject.getString("status"));
                                orderList.add(order);
                            }
                            orderListAdapter.updateData(orderList);
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
                }
            }
        };
    }

    private void setClickListener() {
        ViewHolder.tabOrderType.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0:
                        Value.isUnreceivedOrderPage = true;
                        orderListAdapter.setTakeOrderBtnVisibility(true);
                        break;
                    case 1:
                        Value.isUnreceivedOrderPage = false;
                        orderListAdapter.setTakeOrderBtnVisibility(false);
                        break;
                }
                orderList.clear();
                requestOrder(Value.isUnreceivedOrderPage, Value.orderByIndex);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
        ViewHolder.spinnerSort.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Value.orderByIndex = position;
                orderList.clear();
                requestOrder(Value.isUnreceivedOrderPage, Value.orderByIndex);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void requestOrder(boolean isUnreceivedOrderPage, int orderBy) {
        if (isUnreceivedOrderPage) {
            String url = Value.baseURL + "/order/all/unreceived?" + "orderBy=" + orderBy;
            myRequest.get(url, handler, HandlerWhats.getOrderHandlerWhat);
        } else {
            String url = Value.baseURL + "/order/all/received?" + "orderBy=" + orderBy;
            String key = getString(R.string.shared_preferences_token_key);
            String token = sharedPreferences.getString(key, null);
            myRequest.get(url, handler, HandlerWhats.getOrderHandlerWhat, token);
        }
    }

    private void requestAcceptOrder(String orderNo) {
        String url = Value.baseURL + "/order/accept?orderNo=" + orderNo;
        String key = getString(R.string.shared_preferences_token_key);
        String token = sharedPreferences.getString(key, null);
        myRequest.get(url, handler, HandlerWhats.acceptOrderHandlerWhat, token);
    }
}