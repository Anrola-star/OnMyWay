package com.anrola.onmyway.Utils;

import android.content.Context;
import android.util.Log;

import com.amap.api.services.core.AMapException;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.route.DistanceItem;
import com.amap.api.services.route.DistanceResult;
import com.amap.api.services.route.DistanceSearch;
import com.anrola.onmyway.Entity.Order;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeliveryRouteDynamic {
    private static final String TAG = "DeliveryRouteDynamic";
    // 保存规划过程中的状态
    private static class PlanState {
        Context context;
        List<Order> remainingOrders;
        LatLonPoint currentPos;
        List<Order> deliverySequence;
        List<LatLonPoint> deliveryLatLonPointSequence;
        List<Float> distanceLists;
        List<PlanObject> planResult;
        OnSequencePlannedListener listener;

        PlanState(Context context, List<Order> remainingOrders, LatLonPoint currentPos,
                  List<Order> deliverySequence,
                  List<LatLonPoint> deliveryLatLonPointSequence,
                  List<Float> distanceLists,
                  List<PlanObject> planResult,
                  OnSequencePlannedListener listener) {
            this.context = context;
            this.remainingOrders = remainingOrders;
            this.currentPos = currentPos;
            this.deliverySequence = deliverySequence;
            this.deliveryLatLonPointSequence = deliveryLatLonPointSequence;
            this.distanceLists = distanceLists;
            this.planResult = planResult;
            this.listener = listener;
        }
    }

    public static class PlanObject {
        public Order order;
        public LatLonPoint point;
        public Float distance;
        public Boolean isPickupPoint;
        public PlanObject(Order order, LatLonPoint point, Float distance, Boolean isPickupPoint) {
            this.order = order;
            this.point = point;
            this.distance = distance;
            this.isPickupPoint = isPickupPoint;
        }
    }
    public static void calculateDistance(Context context, ArrayList<LatLonPoint> startLatLonPoints, LatLonPoint end, DistanceSearch.OnDistanceSearchListener listener) {
        try {
            DistanceSearch distanceSearch = new DistanceSearch(context);
            distanceSearch.setDistanceSearchListener(listener);
            DistanceSearch.DistanceQuery distanceQuery = new DistanceSearch.DistanceQuery();
            distanceQuery.setOrigins(startLatLonPoints);
            distanceQuery.setDestination(end);
            distanceQuery.setType(DistanceSearch.TYPE_DRIVING_DISTANCE);
            distanceSearch.calculateRouteDistanceAsyn(distanceQuery);
        } catch (AMapException e) {
            throw new RuntimeException(e);
        }

    }
    public static void calculateDistance(Context context, LatLonPoint start, LatLonPoint end, DistanceSearch.OnDistanceSearchListener listener) {
        ArrayList<LatLonPoint> startLatLonPoints = new ArrayList<>();
        startLatLonPoints.add(start);
        calculateDistance(context, startLatLonPoints, end, listener);
    }

    private static LatLonPoint convertToLatLonPoint(JSONObject jsonObject) {
        try {
            return new LatLonPoint(
                    jsonObject.getDouble("latitude"),
                    jsonObject.getDouble("longitude"));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }



    // 外部回调：配送顺序规划完成后通知
    public interface OnSequencePlannedListener {
        void onSequencePlanned(List<PlanObject> planResult);
        void onPlanFailed(String errorMsg);
    }
    private static class PointOrderMapping {
        LatLonPoint point;    // 可访问点坐标
        Order order;          // 关联的订单
        int index;            // 该点在多起点列表中的索引（匹配高德返回结果）

        PointOrderMapping(LatLonPoint point, Order order, int index) {
            this.point = point;
            this.order = order;
            this.index = index;
        }
    }
    public static void startPlan(Context context, List<Order> orderList,
                                 LatLonPoint start, OnSequencePlannedListener listener) {
        // 入参校验
        if (context == null || orderList == null || orderList.isEmpty() || start == null) {
            listener.onPlanFailed("入参为空");
            return;
        }



        // 初始化状态
        Log.d(TAG, "开始配送点规划...");
        List<Order> remainingOrders = new ArrayList<>();
        for (Order order : orderList){
            if (order == null){
                continue;
            }
            Order orderFake = new Order(order);
            remainingOrders.add(orderFake);
        }
        List<Order> deliverySequence = new ArrayList<>();
        List<LatLonPoint> deliveryLatLonPointSequence = new ArrayList<>();
        List<Float> distanceLists = new ArrayList<>();
        List<PlanObject> planResult = new ArrayList<>();
        PlanState state = new PlanState(
                context, remainingOrders, start,
                deliverySequence,
                deliveryLatLonPointSequence, distanceLists, planResult, listener);

        // 启动第一步规划
        planNextStep(state);
    }

    // 异步规划下一个配送点（核心流程）
    private static void planNextStep(PlanState state) {
        // 终止条件：所有订单完成，回调最终结果
        if (state.remainingOrders.isEmpty()) {
            state.listener.onSequencePlanned(state.planResult);
            return;
        }

        // 步骤1：收集当前所有【合法可访问点】+ 构建【点-订单】映射关系
        // 可访问点规则：待取货订单的取货点 / 已取货订单的送货点（严格先取后送）
        Log.d(TAG, "开始收集可访问点...");
        List<PointOrderMapping> pointOrderMappings = new ArrayList<>();
        ArrayList<LatLonPoint> multiStartPoints = new ArrayList<>(); // 反向使用的多起点列表（所有可访问点）
        int index = 0; // 点索引（匹配高德返回结果的顺序）

        for (Order order : state.remainingOrders) {
            LatLonPoint accessiblePoint;
            if (order.isFinished()) {
                continue;
            }
            if (!order.isPickup()) {
                // 待取货订单：取货点（startLocation）为可访问点
                accessiblePoint = convertToLatLonPoint(order.getStartLocation());
            } else {
                // 已取货订单：送货点（endLocation）为可访问点（已解锁）
                accessiblePoint = convertToLatLonPoint(order.getEndLocation());
            }
            // 构建映射关系：点 + 订单 + 索引
            pointOrderMappings.add(new PointOrderMapping(accessiblePoint, order, index));
            // 添加到反向使用的多起点列表
            multiStartPoints.add(accessiblePoint);
            Log.d(TAG, "添加可访问点：" + accessiblePoint.toString());
            index++;
        }

        // 异常处理：无可访问点（理论上不会出现，防止极端情况）
        if (multiStartPoints.isEmpty()) {
            state.listener.onPlanFailed("当前无可访问的配送点，规划终止");
            return;
        }

        // 步骤2：反向复用多起点计算函数，批量计算当前位置到所有可访问点的距离
        // 核心：单起点（currentPos）→多终点（multiStartPoints） → 反向为 多起点（multiStartPoints）→单终点（currentPos）
        Log.d(TAG, "开始批量计算距离...");
        calculateDistance(
                state.context,                // 上下文
                multiStartPoints,             // 反向：多起点（所有可访问点）
                state.currentPos,             // 反向：单终点（外卖员当前位置）
                new DistanceSearch.OnDistanceSearchListener() {
                    @Override
                    public void onDistanceSearched(DistanceResult distanceResult, int i) {
                        if (i == 1000) {
                            Log.d(TAG, "批量计算距离成功：" + i);
                            handleDistanceResult(distanceResult, i, state, pointOrderMappings);
                        }else {
                            Log.d(TAG, "批量计算距离失败，错误码：" + i);
                            state.listener.onPlanFailed("批量计算距离失败，错误码：" + i);
                        }

                    }
                }
        );
    }

    // 步骤3：处理高德批量距离计算结果，选择最近点并更新规划状态
    private static void handleDistanceResult(DistanceResult result, int rCode,
                                             PlanState state, List<PointOrderMapping> pointOrderMappings) {
        // 高德API返回码：1000为成功（其他为失败，参考高德文档）
        if (rCode != 1000 || result == null || result.getDistanceResults() == null || result.getDistanceResults().isEmpty()) {
            state.listener.onPlanFailed("高德距离计算失败，错误码：" + rCode + "，结果：" + result);
            return;
        }

        // 遍历映射关系，匹配每个点的距离（按索引对应）
        PointOrderMapping targetMapping = null;
        float minDistance = Float.MAX_VALUE;
        for (PointOrderMapping mapping : pointOrderMappings) {
            // 高德返回的DistanceResult列表，顺序与请求的多起点列表顺序完全一致
            DistanceItem distanceItem = result.getDistanceResults().get(mapping.index);
            float distance = distanceItem.getDistance(); // 距离：米（高德默认返回米）

            // 选择距离最近的点
            if (distance < minDistance) {
                minDistance = distance;
                targetMapping = mapping;
            }
        }

        // 异常处理：未找到有效距离结果
        if (targetMapping == null) {
            state.listener.onPlanFailed("未匹配到有效距离结果，规划终止");
            return;
        }

        // 步骤4：更新配送规划状态
        Order targetOrder = targetMapping.order;    // 选中的目标订单
        LatLonPoint targetPoint = targetMapping.point; // 选中的目标点

        Log.d(TAG, "选中配送点：" + targetPoint.toString());
        Log.d(TAG, "选中配送订单：" + targetOrder.getTitle());

        // 1. 添加到配送顺序和距离列表
        state.deliverySequence.add(targetOrder);
        state.distanceLists.add(minDistance);
        state.deliveryLatLonPointSequence.add(targetPoint);
        state.planResult.add(new PlanObject(targetOrder, targetPoint, minDistance,!targetOrder.isPickup()));
        // 2. 更新外卖员当前位置（移动到目标点）
        state.currentPos = targetPoint;
        // 3. 更新订单状态/移除完成订单
        if (!targetOrder.isPickup()) {
            // 取货操作：标记为已取货，解锁送货点（后续变为可访问点）
            targetOrder.setPickup(true);
        } else {
            // 送货操作：订单完成，从剩余列表中移除
            state.remainingOrders.remove(targetOrder);
        }

        // 步骤5：递归规划下一个配送点（异步流程闭环）
        Log.d(TAG, "开始规划下一个配送点...");
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        planNextStep(state);
    }
}
