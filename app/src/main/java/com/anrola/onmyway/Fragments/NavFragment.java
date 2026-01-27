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
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.TextView;

import com.amap.api.maps.model.LatLng;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.route.BusRouteResult;
import com.amap.api.services.route.DriveRouteResult;
import com.amap.api.services.route.RideRouteResult;
import com.amap.api.services.route.RouteSearch;
import com.amap.api.services.route.WalkRouteResult;
import com.anrola.onmyway.Entity.Order;
import com.anrola.onmyway.R;
import com.amap.api.maps.*;
import com.anrola.onmyway.Utils.AMapManager;
import com.anrola.onmyway.Utils.DeliveryRouteDynamic;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class NavFragment extends Fragment {

    private Context context;
    private static final String TAG = "NavFragment";
    private AMapManager amapManager;
    private List<Order> acceptedOrders = new ArrayList<>();

    private static class Value {
        public static LatLng currentLatLng = new LatLng(0, 0);
        public static Boolean isStartLocation = false;
        public static Boolean isShowFloatTips = true;
    }


    private static class ViewHolder {
        private static MapView mapView;
        private static TextView tvSettings;
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
                // 暂时用于测试
                Threads.doDeliveryPlanThread.interrupt();
                Threads.drawOrderRouteThread.interrupt();
                Threads.drawNextOrderRouteThread.interrupt();
                startDeliveryPlan();
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

                    LatLng latLng = new LatLng(
                            amapManager.getAMap().getMyLocation().getLatitude(),
                            amapManager.getAMap().getMyLocation().getLongitude());


                    if (latLng.equals(new LatLng(0, 0))) {
                        continue;
                    }

                    Value.currentLatLng = new LatLng(
                            amapManager.getAMap().getMyLocation().getLatitude(),
                            amapManager.getAMap().getMyLocation().getLongitude());

                    if (!Value.isStartLocation) {
                        Value.isStartLocation = true;
                        amapManager.moveCamera(latLng, 18);
                    }


                    Log.d(TAG,
                            String.format("当前位置：%.6f, %.6f",
                                    amapManager.getAMap().getMyLocation().getLatitude(),
                                    amapManager.getAMap().getMyLocation().getLongitude()
                            )
                    );


                    try {
                        Thread.sleep(1000);
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
                        Thread.sleep(500);
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
                while (true) {
                    if (Thread.currentThread().isInterrupted()) {
                        Log.d(TAG, "drawNextOrderRouteThread: 线程被中断");
                        return;
                    }

                    amapManager.addRoute(
                            new LatLonPoint(Value.currentLatLng.latitude, Value.currentLatLng.longitude),
                            Point, Color.RED, 15,
                            new RouteSearch.OnRouteSearchListener() {
                                @Override
                                public void onBusRouteSearched(BusRouteResult busRouteResult, int i) {

                                }

                                @Override
                                public void onDriveRouteSearched(DriveRouteResult driveRouteResult, int i) {
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
                                }

                                @Override
                                public void onWalkRouteSearched(WalkRouteResult walkRouteResult, int i) {

                                }

                                @Override
                                public void onRideRouteSearched(RideRouteResult rideRouteResult, int i) {

                                }
                            });

                    try {
                        Thread.sleep(5000);
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
                            Thread.sleep(5000);
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

    public void setAcceptedOrders(List<Order> acceptedOrders) {
        this.acceptedOrders = acceptedOrders;
    }

    public void setFloatTipsText(String targetLocationText, String distanceText, boolean isPickup) {

        String action = isPickup ? "取单点" : "送达点";

        String text = String.format("正在前往 %s %s\n距离：%s 米", targetLocationText, action, distanceText);
        ViewHolder.tvFloatText.setText(text);
    }
}