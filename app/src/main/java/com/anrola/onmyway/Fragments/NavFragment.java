package com.anrola.onmyway.Fragments;

import static com.anrola.onmyway.Value.GlobalConstants.DELIVERY_PLAN_THREAD_WAITING_SLEEP_TIME;
import static com.anrola.onmyway.Value.GlobalConstants.DRAW_NEXT_ORDER_ROUTE_THREAD_NO_ACTION_SLEEP_TIME;
import static com.anrola.onmyway.Value.GlobalConstants.DRAW_NEXT_ORDER_ROUTE_THREAD_SLEEP_TIME;
import static com.anrola.onmyway.Value.GlobalConstants.DRAW_ORDER_ROUTE_THREAD_SLEEP_TIME;
import static com.anrola.onmyway.Value.GlobalConstants.MAP_ZOOM_IN_LOCATION;
import static com.anrola.onmyway.Value.GlobalConstants.UPDATE_LOCATION_THREAD_SLEEP_TIME;
import static com.anrola.onmyway.Value.GlobalConstants.WAITING_ORDER_THREAD_SLEEP_TIME;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.LocationManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.maps.model.LatLng;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.route.BusRouteResult;
import com.amap.api.services.route.DrivePath;
import com.amap.api.services.route.DriveRouteResult;
import com.amap.api.services.route.RideRouteResult;
import com.amap.api.services.route.RouteSearch;
import com.amap.api.services.route.WalkRouteResult;
import com.anrola.onmyway.Activities.MainActivity;
import com.anrola.onmyway.Entity.Order;
import com.anrola.onmyway.R;
import com.amap.api.maps.*;
import com.anrola.onmyway.Utils.AMapManager;
import com.anrola.onmyway.Utils.DeliveryRouteDynamic;
import com.anrola.onmyway.Utils.DrivingRouteOverlay;
import com.anrola.onmyway.Utils.MapUtil;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;


public class NavFragment extends Fragment {

    private Context context;
    private static final String TAG = "NavFragment";
    private AMapManager amapManager;
    private List<Order> acceptedOrders = new ArrayList<>();
    private DrivingRouteOverlay nextRouteOverlay;

    private static class Value {
        public static LatLng currentLatLng = new LatLng(0, 0);
        public static Boolean isStartLocation = false;
        public static Boolean isShowFloatTips = true;


    }

    private static class ViewHolder {
        private static MapView mapView;
        private static TextView tvSettings;
        private static TextView tvMyOrders;
        private static Button btnFloat;
        private static Button btnMyLocation;
        private static Button btnRight;
        private static TextView tvFloatText;
        private static CardView cvFloatTips;
    }

    private static class Threads {
        public static Thread updateLocationThread = new Thread();
        public static Thread drawOrderRouteThread = new Thread();
        public static Thread drawNextOrderRouteThread = new Thread();
        public static Thread doDeliveryPlanThread = new Thread();
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_nav, container, false);
        intiView(view);
        setClickListener();
        initMap(view, savedInstanceState);
        return view;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        ViewHolder.mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ViewHolder.mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        ViewHolder.mapView.onLowMemory();
    }

    @Override
    public void onPause() {
        super.onPause();

        if (ViewHolder.mapView != null) {
            ViewHolder.mapView.onPause();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (ViewHolder.mapView != null) {
            ViewHolder.mapView.onResume();
        }
    }

    private void intiView(View view) {
        context = view.getContext();
        ViewHolder.tvSettings = view.findViewById(R.id.tv_setting);
        ViewHolder.tvMyOrders = view.findViewById(R.id.tv_my_orders);
        ViewHolder.btnFloat = view.findViewById(R.id.btn_close_float);
        ViewHolder.btnRight = view.findViewById(R.id.btn_right);
        ViewHolder.tvFloatText = view.findViewById(R.id.tv_float_text);
        ViewHolder.cvFloatTips = view.findViewById(R.id.cv_float_tips);
        ViewHolder.btnMyLocation = view.findViewById(R.id.btn_my_location);
    }

    private void initMap(View view, Bundle savedInstanceState) {

        ViewHolder.mapView = view.findViewById(R.id.fn_map);
        amapManager = new AMapManager(context, ViewHolder.mapView);

        amapManager.initMap(context);
        amapManager.CreateMap(new AMapManager.onMapLoadFinishedListener() {
            @Override
            public void onMapLoadFinished() {

            }
        });
        amapManager.requestMapPermissions(new AMapManager.onLocationPermissionGrantedListener() {
            @Override
            public void onLocationPermissionGranted() {
                if (!AMapManager.isLocationServiceOpened(context)) {
                    // 弹出打开定位服务界面
                    Toast.makeText(context, "请打开定位服务", Toast.LENGTH_SHORT).show();
                    amapManager.openLocationSetting(context);
                }
                amapManager.doLocation();
                startUpdateLocation();
            }
        });
    }

    private void setClickListener() {
        ViewHolder.tvSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO
                // 弹出设置界面


            }
        });
        ViewHolder.tvMyOrders.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });

        ViewHolder.btnFloat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 动画保留不会改变点击判定点，所以复用收起按钮

                Animation Anim;
                AnimationSet animationSet = new AnimationSet(false);
                TranslateAnimation translate;
                if (!Value.isShowFloatTips) {
                    Value.isShowFloatTips = true;
                    translate = new TranslateAnimation(
                            Animation.RELATIVE_TO_PARENT, 0.9f, // fromXDelta：X轴初始位置
                            Animation.RELATIVE_TO_PARENT, 0f, // toXDelta：X轴最终位置
                            Animation.RELATIVE_TO_PARENT, 0f, // fromYDelta：Y轴初始位置
                            Animation.RELATIVE_TO_PARENT, 0f  // toYDelta：Y轴最终位置
                    );
                    ViewHolder.btnRight.setVisibility(View.GONE);
                } else {
                    Value.isShowFloatTips = false;
                    translate = new TranslateAnimation(
                            Animation.RELATIVE_TO_PARENT, 0f, // fromXDelta：X轴初始位置
                            Animation.RELATIVE_TO_PARENT, 0.9f, // toXDelta：X轴最终位置
                            Animation.RELATIVE_TO_PARENT, 0f, // fromYDelta：Y轴初始位置
                            Animation.RELATIVE_TO_PARENT, 0f  // toYDelta：
                    );
                    ViewHolder.btnRight.setVisibility(View.VISIBLE);
                }

                animationSet.addAnimation(translate);
                animationSet.setDuration(300);
                animationSet.setFillAfter(true); // 动画结束后保留
                animationSet.setInterpolator(new AccelerateInterpolator());
                Anim = animationSet;

                ViewHolder.cvFloatTips.startAnimation(Anim);
            }
        });
        ViewHolder.btnMyLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!Value.isStartLocation) {
                    return;
                }
                amapManager.moveCamera(Value.currentLatLng, 18);
            }
        });
    }

    public void startUpdateLocation() {
        Threads.updateLocationThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    if (Thread.currentThread().isInterrupted()) {
                        Log.d(TAG, "updateLocationThread: 线程被中断");
                        return;
                    }

                    if (amapManager.getAMap().getMyLocation() == null) {
                        continue;
                    }

                    LatLng newLatLng = new LatLng(
                            amapManager.getAMap().getMyLocation().getLatitude(),
                            amapManager.getAMap().getMyLocation().getLongitude());


                    if (newLatLng.longitude == 0 || newLatLng.latitude == 0) {
                        continue;
                    }

                    Value.currentLatLng = newLatLng;

                    if (!Value.isStartLocation) {
                        Value.isStartLocation = true;
                        amapManager.moveCamera(Value.currentLatLng, MAP_ZOOM_IN_LOCATION);

                        // 开始计划配送路径
                        requireActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                startDeliveryPlan();
                            }
                        });
                    }


                    Log.d(TAG,
                            String.format("当前位置：%.6f, %.6f",
                                    Value.currentLatLng.longitude,
                                    Value.currentLatLng.latitude
                            )
                    );

                    try {
                        Thread.sleep(UPDATE_LOCATION_THREAD_SLEEP_TIME);
                    } catch (InterruptedException e) {
                        Log.e(TAG, "drawOrderRouteThread: 线程被中断");
                    }


                }


            }
        });
        Threads.updateLocationThread.start();
    }

    public void startDrawOrderPoint(List<LatLonPoint> deliveryLatLonPointSequence, List<DeliveryRouteDynamic.PlanObject> planResult) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    if (amapManager.getAMap().getMyLocation() == null) {
                        continue;
                    }

                    drawOrderPoints(deliveryLatLonPointSequence, planResult);

                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        Log.e(TAG, "startUpdateLocation: " + e);
                    }
                }
            }
        }).start();

    }

    public void drawOrderPoints(List<LatLonPoint> deliveryLatLonPointSequence, List<DeliveryRouteDynamic.PlanObject> planResult) {
        // 绘制配送点
        for (int i = 0; i < deliveryLatLonPointSequence.size(); i++) {
            String title1 = (planResult.get(i).isPickupPoint ? "取单点" : "送达点") + (i + 1) + "\n";
            String title2 = planResult.get(i).order.getTitle() + "\n";
            String title3 = "距离：" + planResult.get(i).distance + "米\n";
            amapManager.addMarker(deliveryLatLonPointSequence.get(i), title1 + title2 + title3);
        }
    }

    public void startDrawOrderRoute(List<LatLonPoint> deliveryLatLonPointSequence) {
        Threads.drawOrderRouteThread = new Thread(new Runnable() {
            @Override
            public void run() {
                if (Thread.currentThread().isInterrupted()) {
                    Log.d(TAG, "drawOrderRouteThread: 线程被中断");
                    return;
                }

                // 绘制配送路线
                for (int i = 0; i < deliveryLatLonPointSequence.size() - 1; i++) {
                    if (Thread.currentThread().isInterrupted()) {
                        return;
                    }

                    amapManager.addRoute(deliveryLatLonPointSequence.get(i), deliveryLatLonPointSequence.get(i + 1), null, 20);
                    try {
                        Thread.sleep(DRAW_ORDER_ROUTE_THREAD_SLEEP_TIME);
                    } catch (InterruptedException e) {
                        Log.e(TAG, "drawOrderRouteThread: 线程被中断");
                    }
                }
            }
        });
        Threads.drawOrderRouteThread.start();
    }

    public void startDrawNextOrderRoute(LatLonPoint Point, DeliveryRouteDynamic.PlanObject planObject) {
        Threads.drawNextOrderRouteThread = new Thread(new Runnable() {
            @Override
            public void run() {
                LatLng oldLatLng = null;
                while (true) {
                    try {
                        if (Thread.currentThread().isInterrupted()) {
                            Log.d(TAG, "drawNextOrderRouteThread: 线程被中断");
                            return;
                        }


                        if (oldLatLng == Value.currentLatLng) {
                            Log.d(TAG, "drawNextOrderRouteThread: 无移动");
                            Thread.sleep(DRAW_NEXT_ORDER_ROUTE_THREAD_NO_ACTION_SLEEP_TIME);
                            continue;
                        } else if (oldLatLng != null) {
                            int d = DrivingRouteOverlay.calculateDistance(oldLatLng, Value.currentLatLng);
                            if (d < 10) {
                                Log.d(TAG, "drawNextOrderRouteThread: 移动,但在误差允许内" + d + "米");
                                Thread.sleep(DRAW_NEXT_ORDER_ROUTE_THREAD_NO_ACTION_SLEEP_TIME);
                                continue;
                            }
                        }

                        amapManager.getRoute(
                                Value.currentLatLng,
                                new LatLng(Point.getLatitude(), Point.getLongitude()),
                                new RouteSearch.OnRouteSearchListener() {
                                    @Override
                                    public void onBusRouteSearched(BusRouteResult busRouteResult, int i) {

                                    }

                                    @Override
                                    public void onDriveRouteSearched(DriveRouteResult driveRouteResult, int i) {
                                        if (i == 1000) {
                                            Log.i(TAG, "驾车路径规划成功");

                                            DrivePath drivePath = driveRouteResult.getPaths().get(0);
                                            if (drivePath == null) {
                                                return;
                                            }

                                            DrivingRouteOverlay newNextRouteOverlay = new DrivingRouteOverlay(
                                                    context, amapManager.getAMap(), drivePath, null);
                                            newNextRouteOverlay.setRouteWidth(15);
                                            newNextRouteOverlay.setIsColorfulline(true);
                                            newNextRouteOverlay.addToMap(Color.RED);
                                            if (nextRouteOverlay != null) {
                                                nextRouteOverlay.removeFromMap();
                                            }
                                            nextRouteOverlay = newNextRouteOverlay;

                                            int dis = (int) drivePath.getDistance();
                                            int dur = (int) drivePath.getDuration();
                                            String des = MapUtil.getFriendlyTime(dur) + "(" + MapUtil.getFriendlyLength(dis) + ")";
                                            Log.d(TAG, des);

                                            String targetLocationText = "";
                                            try {
                                                if (planObject.isPickupPoint) {
                                                    targetLocationText = planObject.order.getStartLocation().getString("name");
                                                } else {
                                                    targetLocationText = planObject.order.getEndLocation().getString("name");
                                                }
                                            } catch (JSONException e) {
                                                throw new RuntimeException(e);
                                            }

                                            setFloatTipsText(
                                                    targetLocationText,
                                                    String.valueOf(driveRouteResult.getPaths().get(0).getDistance()),
                                                    planObject.isPickupPoint);

                                        } else {
                                            Log.e(TAG, "驾车路径规划失败,错误码：" + i);
                                        }
                                    }

                                    @Override
                                    public void onWalkRouteSearched(WalkRouteResult walkRouteResult, int i) {

                                    }

                                    @Override
                                    public void onRideRouteSearched(RideRouteResult rideRouteResult, int i) {

                                    }
                                });
                        oldLatLng = Value.currentLatLng;

                        Thread.sleep(DRAW_NEXT_ORDER_ROUTE_THREAD_SLEEP_TIME);
                    } catch (InterruptedException e) {
                        Log.e(TAG, "drawNextOrderRouteThread: 线程被中断");
                        return;
                    }
                }
            }
        });
        Threads.drawNextOrderRouteThread.start();
    }

    private void startDeliveryPlan() {
        Threads.doDeliveryPlanThread.interrupt();
        Threads.drawOrderRouteThread.interrupt();
        Threads.drawNextOrderRouteThread.interrupt();

        if (acceptedOrders.isEmpty()) { // 无订单, 请求订单
            MainActivity mainActivity = (MainActivity) requireActivity();
            OrderFragment orderFragment = (OrderFragment) mainActivity.getOrderFragment();
            orderFragment.requestAcceptOrderInNavFragment();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (acceptedOrders.isEmpty()) {
                        try {
                            System.out.println("等待订单...");
                            Thread.sleep(WAITING_ORDER_THREAD_SLEEP_TIME);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    startDeliveryPlan();    //再次尝试
                }
            }).start();
        }else {  // 有订单, 开始规划路线
            Log.d(TAG, "收到订单，开始规划路线");
            Threads.doDeliveryPlanThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true) {

                        if (Thread.currentThread().isInterrupted()) {
                            Log.d(TAG, "doDeliveryPlanThread: 线程被中断");
                            return;
                        }

                        if (!Value.isStartLocation) {
                            try {
                                Thread.sleep(DELIVERY_PLAN_THREAD_WAITING_SLEEP_TIME);
                            } catch (InterruptedException e) {
                                Log.e(TAG, "startUpdateLocation: " + e);
                            }
                            continue;
                        }

                        DeliveryRouteDynamic.startPlan(
                                context,
                                acceptedOrders,
                                new LatLonPoint(Value.currentLatLng.latitude, Value.currentLatLng.longitude),
                                new DeliveryRouteDynamic.OnSequencePlannedListener() {
                                    @Override
                                    public void onSequencePlanned(List<DeliveryRouteDynamic.PlanObject> planResult) {

                                        if (planResult.isEmpty()) {
                                            return;
                                        }

                                        List<LatLonPoint> deliveryLatLonPointSequence = new ArrayList<>();
                                        for (int i = 0; i < planResult.size(); i++) {
                                            Order order = planResult.get(i).order;
                                            LatLonPoint point = planResult.get(i).point;
                                            deliveryLatLonPointSequence.add(point);
                                        }

                                        Log.d("DeliveryRouteDynamic", "配送序列： " + deliveryLatLonPointSequence);
                                        //amapManager.addPolyline(deliveryLatLonPointSequence);

                                        amapManager.clear();
                                        startDrawOrderRoute(deliveryLatLonPointSequence);
                                        startDrawNextOrderRoute(deliveryLatLonPointSequence.get(0), planResult.get(0));

                                    }

                                    @Override
                                    public void onPlanFailed(String errorMsg) {

                                    }
                                });
                        break;
                    }
                }
            });
            Threads.doDeliveryPlanThread.start();
        }
    }

    public void setAcceptedOrders(List<Order> acceptedOrders) {
        this.acceptedOrders = acceptedOrders;
    }

    public void setFloatTipsText(String targetLocationText, String distanceText, boolean isPickup) {

        String action = isPickup ? "取单点" : "送达点";

        String text = String.format("正在前往 %s %s\n距离：%s 米", targetLocationText, action, distanceText);
        ViewHolder.tvFloatText.setText(text);
    }


}