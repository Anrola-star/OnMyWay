package com.anrola.onmyway.Adapter;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.anrola.onmyway.Entity.Order;
import com.anrola.onmyway.R;

import org.json.JSONException;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 订单列表适配器（适配 RecyclerView，用于 ViewPager2 承载）
 */
public class OrderListAdapter extends RecyclerView.Adapter<OrderListAdapter.OrderViewHolder> {

    private final Context context;
    private List<Order> orderList;

    private final String TAG = "OrderListAdapter";

    // 接单按钮点击事件回调
    private OnTakeOrderClickListener onTakeOrderClickListener;
    private OnPickOrderClickListener onPickOrderClickListener;
    private OnDoneOrderClickListener onDoneOrderClickListener;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Runnable refreshItemRunnable;

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
        Order currentOrder = orderList.get(position);
        // 订单为空
        if (currentOrder == null) {
            return;
        }

        // 展示订单
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
        holder.tvOrderDistance.setText(String.format("距离：%s 米", currentOrder.getDistance()));

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
                int realPosition = getPositionByOderNo(currentOrder.getNo());
                onTakeOrderClickListener.onTakeOrderClick(realPosition, currentOrder);
            }
        });
        // 取货按钮点击事件
        holder.btnPickOrder.setOnClickListener(v -> {
            if (onPickOrderClickListener != null) {
                int realPosition = getPositionByOderNo(currentOrder.getNo());
                onPickOrderClickListener.onPickOrderClick(realPosition, currentOrder);
            }
        });
        // 送达按钮点击事件
        holder.btnDoneOrder.setOnClickListener(v -> {
            if (onDoneOrderClickListener != null) {
                int realPosition = getPositionByOderNo(currentOrder.getNo());
                onDoneOrderClickListener.onDoneOrderClick(realPosition, currentOrder);
            }
        });
    }

    @Override
    public int getItemCount() {
        return orderList == null ? 0 : orderList.size();
    }

    @Override
    public void onViewRecycled(@NonNull OrderViewHolder holder) {
        super.onViewRecycled(holder);
        resetViewHolder(holder);
    }

    public void updateData(List<Order> newOrderList) {
        int oldSize = this.orderList == null ? 0 : this.orderList.size();
        this.orderList.clear();
        if (newOrderList != null) {
            this.orderList.addAll(newOrderList);
        }
        int newSize = this.orderList.size();

        if (oldSize > 0) {
            notifyItemRangeRemoved(0, oldSize);
        }
        if (newSize > 0) {
            notifyItemRangeInserted(0, newSize);
        }
    }

    public void updateOrderToAccepted(String orderNo, boolean isAccepted) {
        int position = getPositionByOderNo(orderNo);
        if (position >= 0 && position < orderList.size()) {
            Order updatedOrder = orderList.get(position);
            updatedOrder.setAccepted(isAccepted);
        }
    }

    public void updateOrderToPicked(String orderNo, boolean isPicked) {
        int position = getPositionByOderNo(orderNo);
        if (position >= 0 && position < orderList.size()) {
            Order updatedOrder = orderList.get(position);
            updatedOrder.setPickup(isPicked);
        }
    }

    public void updateOrderToFinished(String orderNo, boolean isFinished) {
        int position = getPositionByOderNo(orderNo);
        if (position >= 0 && position < orderList.size()) {
            Order updatedOrder = orderList.get(position);
            updatedOrder.setFinished(isFinished);
        }
    }

    public void resetViewHolder(OrderViewHolder holder){
        holder.tvOrderId.setText("");
        holder.tvOrderTitle.setText("");
        holder.tvOrderAmount.setText("");
        holder.tvOrderTime.setText("");
        holder.tvOrderPublisher.setText("");
        holder.tvOrderFromAndTo.setText("");
        holder.tvOrderDistance.setText("");
        holder.tvOrderStatus.setText("");
        holder.btnTakeOrder.setVisibility(View.GONE);
        holder.btnPickOrder.setVisibility(View.GONE);
        holder.btnDoneOrder.setVisibility(View.GONE);
        holder.btnTakeOrder.setOnClickListener(null);
        holder.btnPickOrder.setOnClickListener(null);
        holder.btnDoneOrder.setOnClickListener(null);
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

    public void setOrderList(List<Order> orderList) {
        this.orderList = orderList;
    }
    public void setOnTakeOrderClickListener(OnTakeOrderClickListener listener) {
        this.onTakeOrderClickListener = listener;
    }
    public void setOnPickOrderClickListener(OnPickOrderClickListener listener) {
        this.onPickOrderClickListener = listener;
    }
    public void setOnDoneOrderClickListener(OnDoneOrderClickListener listener) {
        this.onDoneOrderClickListener = listener;
    }
    public OnTakeOrderClickListener getOnTakeOrderClickListener() {
        return onTakeOrderClickListener;
    }

    public OnPickOrderClickListener getOnPickOrderClickListener() {
        return onPickOrderClickListener;
    }

    public OnDoneOrderClickListener getOnDoneOrderClickListener() {
        return onDoneOrderClickListener;
    }
    public int getPositionByOderNo(String orderNo) {
        for (int i = 0; i < orderList.size(); i++) {
            Order order = orderList.get(i);
            if(order ==  null){
                continue;
            }
            if (order.getNo().equals(orderNo)) {
                return i;
            }
        }
        return -1;
    }
    public Order getOrderByOderNo(String orderNo) {
        for (Order order : orderList) {
            if (order ==  null){
                continue;
            }
            if (order.getNo().equals(orderNo)) {
                return order;
            }
        }
        return null;
    }
}
