package com.anrola.onmyway.Fragments;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.amap.api.maps.model.LatLng;
import com.amap.api.services.core.LatLonPoint;
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
        public static LatLng currentLatLng = new LatLng(0,0);
        public static Boolean isStartLocation = false;
    }


    private static class ViewHolder {
        private static MapView mapView;
        private static TextView tvSettings;
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
                startDeliveryPlan();
            }
        });
    }

    public void startUpdateLocation() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    ;
                    if (amapManager.getAMap().getMyLocation() == null) {
                        continue;
                    }


                    Value.currentLatLng = new LatLng(
                            amapManager.getAMap().getMyLocation().getLatitude(),
                            amapManager.getAMap().getMyLocation().getLongitude());
                    Value.isStartLocation = true;
                    Log.d(TAG,
                            String.format("当前位置：%.6f, %.6f",
                                    amapManager.getAMap().getMyLocation().getLatitude(),
                                    amapManager.getAMap().getMyLocation().getLongitude()
                            )
                    );


                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        Log.e(TAG, "startUpdateLocation: "+ e);
                    }


                }


            }
        }).start();
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
                        Log.e(TAG, "startUpdateLocation: "+ e);
                    }
                }
            }
        }).start();

    }
    public void drawOrderPoints(List<LatLonPoint> deliveryLatLonPointSequence, List<DeliveryRouteDynamic.PlanObject> planResult) {
        // 绘制配送点
        for (int i = 0; i < deliveryLatLonPointSequence.size(); i++){
            String title1 = (planResult.get(i).isPickupPoint ? "取单点" : "送达点") +  (i+1) + "\n";
            String title2 = planResult.get(i).order.getTitle()+"\n";
            String title3 = "距离：" + planResult.get(i).distance+"米\n";
            amapManager.addMarker(deliveryLatLonPointSequence.get(i),title1 + title2 + title3);
        }
    }

    public void startDrawOrderRoute(List<LatLonPoint> deliveryLatLonPointSequence) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                // 绘制配送路线
                amapManager.addRoute(new LatLonPoint(Value.currentLatLng.latitude, Value.currentLatLng.longitude), deliveryLatLonPointSequence.get(0),null,20);
                for (int i = 0; i < deliveryLatLonPointSequence.size() - 1; i++){
                    amapManager.addRoute(deliveryLatLonPointSequence.get(i), deliveryLatLonPointSequence.get(i + 1), null,20);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Log.e(TAG, "startUpdateLocation: "+ e);
                    }
                }
                startDrawNextOrderRoute(deliveryLatLonPointSequence);
            }
        }).start();
    }
    public void startDrawNextOrderRoute(List<LatLonPoint> deliveryLatLonPointSequence){
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    amapManager.addRoute(
                            new LatLonPoint(Value.currentLatLng.latitude, Value.currentLatLng.longitude),
                            deliveryLatLonPointSequence.get(0), Color.RED, 15);

                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        Log.e(TAG, "startUpdateLocation: "+ e);
                    }
                }
            }
        }).start();
    }

    private void startDeliveryPlan(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    if (!Value.isStartLocation || Objects.equals(Value.currentLatLng, new LatLng(0, 0))){
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e) {
                            Log.e(TAG, "startUpdateLocation: "+ e);
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
                                }

                                @Override
                                public void onPlanFailed(String errorMsg) {

                                }
                            });
                    break;
                }
            }
        }).start();


    }

    public void setAcceptedOrders(List<Order> acceptedOrders) {
        this.acceptedOrders = acceptedOrders;
    }
}