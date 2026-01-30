package com.anrola.onmyway.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.amap.api.maps.model.LatLng;
import com.amap.api.services.core.LatLonPoint;
import com.anrola.onmyway.Entity.Order;
import com.anrola.onmyway.R;
import com.anrola.onmyway.Utils.DrivingRouteOverlay;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

/**
 * 订单列表适配器（适配 RecyclerView，用于 ViewPager2 承载）
 */
public class OrderListAdapter extends RecyclerView.Adapter<OrderListAdapter.OrderViewHolder> {

    private Context context;
    private List<Order> orderList;
    // 接单按钮点击事件回调
    private OnTakeOrderClickListener onTakeOrderClickListener;
    private OnPickOrderClickListener onPickOrderClickListener;
    private OnDoneOrderClickListener onDoneOrderClickListener;

    // 构造方法
    public OrderListAdapter(Context context, List<Order> orderList) {
        this.context = context;
        this.orderList = orderList;
    }


    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(context)
                .inflate(R.layout.item_order_list, parent, false);
        return new OrderViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        // 获取当前位置的订单数据
        Order currentOrder = orderList.get(position);
        if (currentOrder == null) {
            return;
        }
        String startLocationName = "";
        String endLocationName = "";
        try {
            startLocationName = currentOrder.getStartLocation().getString("name");
            endLocationName = currentOrder.getEndLocation().getString("name");
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        // 填充数据到控件
        holder.tvOrderId.setText(String.format("订单编号：%s", currentOrder.getNo()));
        holder.tvOrderTitle.setText(currentOrder.getTitle());
        holder.tvOrderAmount.setText(String.valueOf(currentOrder.getAmount()));
        holder.tvOrderTime.setText(currentOrder.getStartTime());
        holder.tvOrderPublisher.setText(String.format("接收人：%s", currentOrder.getReceiverName()));
        holder.tvOrderFromAndTo.setText(String.format("从 %s 到 %s", startLocationName, endLocationName));
        LatLng startLocation = new LatLng(0, 0);
        LatLng endLocation = new LatLng(0, 0);
        try {
            startLocation = new LatLng(
                    currentOrder.getStartLocation().getDouble("latitude"),
                    currentOrder.getStartLocation().getDouble("longitude"));
            endLocation = new LatLng(
                    currentOrder.getEndLocation().getDouble("latitude"),
                    currentOrder.getEndLocation().getDouble("longitude"));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        int distance = DrivingRouteOverlay.calculateDistance(startLocation, endLocation);
        holder.tvOrderDistance.setText(String.format("距离：%s 米", distance));

        if (currentOrder.isAccepted()) {
            if (currentOrder.isPickup()) {
                holder.tvOrderStatus.setText("配送中");
            } else {
                holder.tvOrderStatus.setText("待取单");
            }
        } else {
            holder.tvOrderStatus.setText("待接单");
        }

        if (currentOrder.isAccepted()) {
            holder.btnTakeOrder.setVisibility(View.GONE);
            if (currentOrder.isPickup()) {
                holder.btnPickOrder.setVisibility(View.GONE);
                holder.btnDoneOrder.setVisibility(View.VISIBLE);
            } else {
                holder.btnPickOrder.setVisibility(View.VISIBLE);
                holder.btnDoneOrder.setVisibility(View.GONE);
            }
        } else {
            holder.btnTakeOrder.setVisibility(View.VISIBLE);
            holder.btnPickOrder.setVisibility(View.GONE);
            holder.btnDoneOrder.setVisibility(View.GONE);
        }


        // 接单按钮点击事件
        holder.btnTakeOrder.setOnClickListener(v -> {
            if (onTakeOrderClickListener != null) {
                onTakeOrderClickListener.onTakeOrderClick(position, currentOrder);
            }
        });
        // 取货按钮点击事件
        holder.btnPickOrder.setOnClickListener(v -> {
            if (onPickOrderClickListener != null) {
                onPickOrderClickListener.onPickOrderClick(position, currentOrder);
            }
        });
        // 送达按钮点击事件
        holder.btnDoneOrder.setOnClickListener(v -> {
            if (onDoneOrderClickListener != null) {
                onDoneOrderClickListener.onDoneOrderClick(position, currentOrder);
            }
        });
    }

    @Override
    public int getItemCount() {
        return orderList == null ? 0 : orderList.size();
    }

    public void updateData(List<Order> newOrderList) {
        this.orderList = newOrderList;
        notifyDataSetChanged(); // 通知列表数据刷新
    }

    public void removeOrder(int position) {
        if (orderList != null && position >= 0 && position < orderList.size()) {
            orderList.remove(position);
            notifyItemRemoved(position);
        }
    }

    public void updateOrderAccepted(String orderNo, boolean isAccepted) {
        int position = getPositionByOderNo(orderNo);
        if (position >= 0 && position < orderList.size()) {
            Order updatedOrder = orderList.get(position);
            updatedOrder.setAccepted(isAccepted);
            notifyItemChanged(position);
        }
    }

    public void updateOrderPicked(String orderNo, boolean isPicked) {
        int position = getPositionByOderNo(orderNo);
        if (position >= 0 && position < orderList.size()) {
            Order updatedOrder = orderList.get(position);
            updatedOrder.setPickup(isPicked);
            notifyItemChanged(position);
        }
    }

    public void updateOrderFinished(String orderNo, boolean isFinished) {
        int position = getPositionByOderNo(orderNo);
        if (position >= 0 && position < orderList.size()) {
            Order updatedOrder = orderList.get(position);
            updatedOrder.setFinished(isFinished);
            notifyItemChanged(position);
        }
    }

    public static class OrderViewHolder extends RecyclerView.ViewHolder {
        // 订单控件
        TextView tvOrderId;
        TextView tvOrderTitle;
        TextView tvOrderAmount;
        TextView tvOrderTime;
        TextView tvOrderPublisher;
        TextView tvOrderFromAndTo;
        TextView tvOrderDistance;
        TextView tvOrderStatus;
        Button btnTakeOrder;
        Button btnPickOrder;
        Button btnDoneOrder;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            // 绑定控件
            tvOrderId = itemView.findViewById(R.id.tv_order_id);
            tvOrderTitle = itemView.findViewById(R.id.tv_order_title);
            tvOrderAmount = itemView.findViewById(R.id.tv_order_amount);
            tvOrderTime = itemView.findViewById(R.id.tv_order_time);
            tvOrderPublisher = itemView.findViewById(R.id.tv_order_publisher);
            tvOrderDistance = itemView.findViewById(R.id.tv_order_distance);
            btnTakeOrder = itemView.findViewById(R.id.btn_take_order);
            btnPickOrder = itemView.findViewById(R.id.btn_pick_order);
            tvOrderStatus = itemView.findViewById(R.id.tv_order_status);
            btnDoneOrder = itemView.findViewById(R.id.btn_done_order);
            tvOrderFromAndTo = itemView.findViewById(R.id.tv_order_fromAndTo);
        }


    }

    public interface OnTakeOrderClickListener {
        /**
         * 接单点击回调
         *
         * @param position 点击的订单位置
         * @param order    点击的订单数据
         */
        void onTakeOrderClick(int position, Order order);
    }

    public interface OnPickOrderClickListener {
        /**
         * 领单点击回调
         *
         * @param position 点击的订单位置
         * @param order    点击的订单数据
         */
        void onPickOrderClick(int position, Order order);
    }

    public interface OnDoneOrderClickListener {
        /**
         * 订单完成点击回调
         *
         * @param position 点击的订单位置
         * @param order    点击的订单数据
         */
        void onDoneOrderClick(int position, Order order);
    }

    /**
     * 设置接单按钮点击事件
     */
    public void setOnTakeOrderClickListener(OnTakeOrderClickListener listener) {
        this.onTakeOrderClickListener = listener;
    }

    public void setOnPickOrderClickListener(OnPickOrderClickListener listener) {
        this.onPickOrderClickListener = listener;
    }

    public void setOnDoneOrderClickListener(OnDoneOrderClickListener listener) {
        this.onDoneOrderClickListener = listener;
    }

    public int getPositionByOderNo(String orderNo) {
        for (int i = 0; i < orderList.size(); i++) {
            Order order = orderList.get(i);
            if (order.getNo().equals(orderNo)) {
                return i;
            }
        }
        return -1;
    }

    public Order getOrderByOderNo(String orderNo) {
        for (Order order : orderList) {
            if (order.getNo().equals(orderNo)) {
                return order;
            }
        }
        return null;
    }
}
