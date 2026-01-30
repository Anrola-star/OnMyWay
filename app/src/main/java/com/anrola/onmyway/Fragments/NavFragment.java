package com.anrola.onmyway.Fragments;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

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

    private static class Values {
        public static LatLng currentLatLng = new LatLng(0, 0);
        public static Boolean isStartLocation = false;
        public static Boolean isShowFloatTips = true;
        private static final Object waitOrderRequestLock = new Object();
        private static final Object waitStartLocationLock = new Object();
        private static final Object waitRouteRePlanLock = new Object();

        private static final int DRAW_NEXT_ORDER_ROUTE_THREAD_SLEEP_TIME = 1500;
        private static final int DRAW_NEXT_ORDER_ROUTE_THREAD_NO_ACTION_SLEEP_TIME = 500;
        private static final int DELIVERY_PLAN_THREAD_WAITING_SLEEP_TIME = 1000;
        private static final int DRAW_ORDER_ROUTE_THREAD_SLEEP_TIME = 500;
        private static final int UPDATE_LOCATION_THREAD_SLEEP_TIME = 1000;
        private static final int MAP_ZOOM_IN_LOCATION = 18;
    }

    private static class ViewHolder {
        private MapView mapView;
        private TextView tvSettings;
        private TextView tvMyOrders;
        private Button btnFloat;
        private Button btnMyLocation;
        private Button btnRight;
        private TextView tvFloatText;
        private CardView cvFloatTips;
    }

    private final ViewHolder ViewHolder = new ViewHolder();

    private static class Threads {
        public static Thread updateLocationThread;
        public static Thread drawOrderRouteThread;
        public static Thread drawNextOrderRouteThread;
        public static Thread doDeliveryPlanThread;
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
                if (!Values.isShowFloatTips) {
                    Values.isShowFloatTips = true;
                    translate = new TranslateAnimation(
                            Animation.RELATIVE_TO_PARENT, 0.9f, // fromXDelta：X轴初始位置
                            Animation.RELATIVE_TO_PARENT, 0f, // toXDelta：X轴最终位置
                            Animation.RELATIVE_TO_PARENT, 0f, // fromYDelta：Y轴初始位置
                            Animation.RELATIVE_TO_PARENT, 0f  // toYDelta：Y轴最终位置
                    );
                    ViewHolder.btnRight.setVisibility(View.GONE);
                } else {
                    Values.isShowFloatTips = false;
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
                if (!Values.isStartLocation) {
                    return;
                }
                amapManager.moveCamera(Values.currentLatLng, 18);
            }
        });
    }

    public void startUpdateLocation() {
        Threads.updateLocationThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!Thread.currentThread().isInterrupted()) {
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

                    // 已经开始定位，位置持续更新
                    Values.currentLatLng = newLatLng;


                    if (!Values.isStartLocation) {
                        Values.isStartLocation = true;

                        Log.i(TAG, "——持续更新定位已开启——");
                        synchronized (Values.waitStartLocationLock){
                            Values.waitStartLocationLock.notifyAll();// 唤醒等待定位的线程
                        }
                        amapManager.moveCamera(Values.currentLatLng, Values.MAP_ZOOM_IN_LOCATION);

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
                                    Values.currentLatLng.longitude,
                                    Values.currentLatLng.latitude
                            )
                    );

                    try {
                        Thread.sleep(Values.UPDATE_LOCATION_THREAD_SLEEP_TIME);
                    } catch (InterruptedException e) {
                        Log.e(TAG, "drawOrderRouteThread: 线程被中断");
                    }
                }
            }
        });
        Threads.updateLocationThread.start();
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
                        Thread.sleep(Values.DRAW_ORDER_ROUTE_THREAD_SLEEP_TIME);
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
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        if (Thread.currentThread().isInterrupted()) {
                            Log.d(TAG, "drawNextOrderRouteThread: 线程被中断");
                            return;
                        }


                        if (oldLatLng == Values.currentLatLng) {
                            Log.d(TAG, "drawNextOrderRouteThread: 无移动");
                            Thread.sleep(Values.DRAW_NEXT_ORDER_ROUTE_THREAD_NO_ACTION_SLEEP_TIME);
                            continue;
                        } else if (oldLatLng != null) {
                            int d = DrivingRouteOverlay.calculateDistance(oldLatLng, Values.currentLatLng);
                            if (d < 10) {
                                Log.d(TAG, "drawNextOrderRouteThread: 移动,但在误差允许内" + d + "米");
                                Thread.sleep(Values.DRAW_NEXT_ORDER_ROUTE_THREAD_NO_ACTION_SLEEP_TIME);
                                continue;
                            }
                        }

                        amapManager.getRoute(
                                Values.currentLatLng,
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
                        oldLatLng = Values.currentLatLng;

                        Thread.sleep(Values.DRAW_NEXT_ORDER_ROUTE_THREAD_SLEEP_TIME);
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
        if (Threads.doDeliveryPlanThread != null) {
            Threads.doDeliveryPlanThread.interrupt();
        }
        if (Threads.drawOrderRouteThread != null) {
            Threads.drawOrderRouteThread.interrupt();
        }
        if (Threads.drawNextOrderRouteThread != null) {
            Threads.drawNextOrderRouteThread.interrupt();
        }

        if (acceptedOrders.isEmpty()) { // 无订单, 请求订单
            MainActivity mainActivity = (MainActivity) requireActivity();
            OrderFragment orderFragment = (OrderFragment) mainActivity.getOrderFragment();
            orderFragment.requestAndUpdateAcceptOrderInNavFragment();


            new Thread(new Runnable() {
                @Override
                public void run() {
                    // 等待订单获取
                    synchronized (Values.waitOrderRequestLock) {
                        while (acceptedOrders.isEmpty()) {
                            try {
                                System.out.println("等待订单获取...");
                                Values.waitOrderRequestLock.wait();
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                throw new RuntimeException(e);
                            }
                        }
                    }
                    // 订单获取成功
                    Log.i(TAG, "——订单获取成功——");
                    // 再次尝试规划路线
                    startDeliveryPlan();
                }
            }).start();
        } else {  // 有订单, 开始规划路线
            Log.d(TAG, "收到订单，开始规划路线");
            Threads.doDeliveryPlanThread = new Thread(new Runnable() {
                @Override
                public void run() {

                    if (Thread.currentThread().isInterrupted()) {
                        Log.d(TAG, "doDeliveryPlanThread: 线程被中断");
                        return;
                    }

                    // 等待开始位置获取
                    Log.i(TAG, "等待开始位置获取...");
                    synchronized (Values.waitStartLocationLock) {
                        while (!Values.isStartLocation) {
                            try {
                                Values.waitStartLocationLock.wait();
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                throw new RuntimeException(e);
                            }
                        }
                        Log.i(TAG, "——位置获取成功——");
                    }

                    DeliveryRouteDynamic.startPlan(
                            context,
                            acceptedOrders,
                            new LatLonPoint(Values.currentLatLng.latitude, Values.currentLatLng.longitude),
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

                    //TODO: 阻塞线程，订单更新时调用notify唤醒，再次计划
                }
            });
            Threads.doDeliveryPlanThread.start();
        }
    }

    public void notifyDeliveryPlanLock() {
        synchronized (Values.waitOrderRequestLock) {
            Values.waitOrderRequestLock.notifyAll();
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