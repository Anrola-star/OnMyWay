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
import java.util.List;

public class OrderFragment extends Fragment {
    private Context context;
    private MyRequest myRequest = new MyRequest();
    private Handler handler;
    private SharedPreferences sharedPreferences;

    private List<Order> orderList = new ArrayList<>();
    private OrderListAdapter orderListAdapter;
    private String baseURL;

    private static class ViewHolder {
        private static RecyclerView vpOrderList;
        private static TabLayout tabOrderType;
    }

    private final String TAG = "OrderFragment";

    private final int getOrderHandlerWhat = 1;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_order, container, false);

        initViews(view);
        initHandler();
        requestUnreceivedOrder();
        setClickListener();
        initRecyclerView();

        return view;
    }

    private void initViews(View view) {
        context = view.getContext();
        baseURL = myRequest.getBaseURL(context);
        sharedPreferences = requireActivity().getSharedPreferences(
                getString(R.string.shared_preferences_user_info_key),
                Context.MODE_PRIVATE);
        ViewHolder.vpOrderList = view.findViewById(R.id.of_vp_order_list);
        ViewHolder.tabOrderType = view.findViewById(R.id.of_tab_order_type);
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
        orderListAdapter.setOnTakeOrderClickListener((position, orderBean) -> {
            //TODO
            Toast.makeText(getContext(), "已接单：" + orderBean.getNo(), Toast.LENGTH_SHORT).show();
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
                    Log.e("IncomeError", "JSON解析失败：响应字符串为空");
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle("错误")
                            .setMessage("请求失败, 无响应")
                            .setPositiveButton("确定", null)
                            .show();
                    return; // 终止后续解析逻辑
                }


                // 响应数据处理
                switch (msg.what) {
                    case getOrderHandlerWhat: // 获取未接单订单
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
                            throw new RuntimeException(e);
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
                        orderList.clear();
                        requestUnreceivedOrder();
                        break;
                    case 1:
                        orderList.clear();
                        requestReceivedOrder();
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
    }

    private void requestUnreceivedOrder() {
        String url = baseURL + "/order/all/received";
        myRequest.get(url, handler, getOrderHandlerWhat);
    }

    private void requestReceivedOrder() {
        String url = baseURL + "/order/all/unreceived";
        String token = sharedPreferences.getString(getString(R.string.shared_preferences_token_key), "");
        myRequest.get(url, handler, getOrderHandlerWhat, token);
    }
}