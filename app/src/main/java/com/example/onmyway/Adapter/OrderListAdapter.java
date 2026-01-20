package com.example.onmyway.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.onmyway.Entity.Order;
import com.example.onmyway.R;

import java.util.List;

/**
 * 订单列表适配器（适配 RecyclerView，用于 ViewPager2 承载）
 */
public class OrderListAdapter extends RecyclerView.Adapter<OrderListAdapter.OrderViewHolder> {

    private Context context;
    private List<Order> orderList;
    // 接单按钮点击事件回调
    private OnTakeOrderClickListener onTakeOrderClickListener;

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

        // 填充数据到控件
        holder.tvOrderId.setText("订单ID：" + currentOrder.getNo());
        holder.tvOrderTitle.setText(currentOrder.getTitle());
        holder.tvOrderAmount.setText(String.valueOf(currentOrder.getAmount()));
        holder.tvOrderTime.setText(currentOrder.getStartTime());
        holder.tvOrderPublisher.setText("发布人：" + currentOrder.getReceiverName());
        holder.tvOrderDistance.setText("距离：" + currentOrder.getDistance());

        // 接单按钮点击事件
        holder.btnTakeOrder.setOnClickListener(v -> {
            if (onTakeOrderClickListener != null) {
                onTakeOrderClickListener.onTakeOrderClick(position, currentOrder);
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

    public static class OrderViewHolder extends RecyclerView.ViewHolder {
        // 订单控件
        TextView tvOrderId;
        TextView tvOrderTitle;
        TextView tvOrderAmount;
        TextView tvOrderTime;
        TextView tvOrderPublisher;
        TextView tvOrderDistance;
        Button btnTakeOrder;

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
        }
    }

    public interface OnTakeOrderClickListener {
        /**
         * 接单点击回调
         * @param position 点击的订单位置
         * @param order 点击的订单数据
         */
        void onTakeOrderClick(int position, Order order);
    }

    /**
     * 设置接单按钮点击事件
     */
    public void setOnTakeOrderClickListener(OnTakeOrderClickListener listener) {
        this.onTakeOrderClickListener = listener;
    }
}