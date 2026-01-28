package com.anrola.onmyway.Utils;

import static androidx.core.content.ContextCompat.checkSelfPermission;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.MapsInitializer;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.LatLngBounds;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;

import android.Manifest;
import android.widget.Toast;

import com.amap.api.maps.model.Polyline;
import com.amap.api.maps.model.PolylineOptions;
import com.amap.api.services.core.AMapException;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.route.BusRouteResult;
import com.amap.api.services.route.BusRouteResultV2;
import com.amap.api.services.route.DrivePath;
import com.amap.api.services.route.DrivePathV2;
import com.amap.api.services.route.DriveRouteResult;
import com.amap.api.services.route.DriveRouteResultV2;
import com.amap.api.services.route.DriveStep;
import com.amap.api.services.route.DriveStepV2;
import com.amap.api.services.route.RideRouteResult;
import com.amap.api.services.route.RideRouteResultV2;
import com.amap.api.services.route.RouteSearch;
import com.amap.api.services.route.RouteSearchV2;
import com.amap.api.services.route.WalkPath;
import com.amap.api.services.route.WalkRouteResult;
import com.amap.api.services.route.WalkRouteResultV2;
import com.anrola.onmyway.R;

import java.util.ArrayList;
import java.util.List;

public class AMapManager {
    private Context context;
    private MapView mapView;
    private AMap aMap;
    public static class RouteSearchStrategy{
        private final int Fastest = RouteSearch.DRIVING_SINGLE_DEFAULT;
        private final int Shortest = RouteSearch.DRIVING_SINGLE_SHORTEST;
        private final int Default = RouteSearch.DRIVING_MULTI_STRATEGY_FASTEST_SHORTEST_AVOID_CONGESTION;
        private int NowStrategy = Default;

        public void setFastestStrategy(){
            NowStrategy = Fastest;
        }
        public void setShortestStrategy(){
            NowStrategy = Shortest;
        }
        public void setDefaultStrategy(){
            NowStrategy = Default;
        }
        public int getStrategy(){
            return NowStrategy;
        }
    }
    private RouteSearchStrategy routeSearchStrategy = new RouteSearchStrategy();
    private static final String TAG = AMapManager.class.getSimpleName();

    // 权限请求码，用于回调中识别
    private static final int REQUEST_LOCATION_PERMISSION = 1001;

    public AMapManager(Context context, MapView mapView) {

        this.context = context;
        this.mapView = mapView;
    }

    public void initMap(Context context) {
        // 第一步：设置隐私政策是否弹窗告知用户
        MapsInitializer.updatePrivacyShow(context, true, true);
        // 第二步：设置用户是否同意隐私政策
        MapsInitializer.updatePrivacyAgree(context, true);
    }

    public void requestMapPermissions(onLocationPermissionGrantedListener onLocationPermissionGrantedListener) {
        String[] sList = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
        ActivityCompat.requestPermissions((Activity) context, sList, REQUEST_LOCATION_PERMISSION);

        int hasFineLocationPermission = checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION);
        int hasCoarseLocationPermission = checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION);
        boolean isAllGranted = hasFineLocationPermission == PackageManager.PERMISSION_GRANTED &&
                hasCoarseLocationPermission == PackageManager.PERMISSION_GRANTED;
        if (isAllGranted) {
            onLocationPermissionGrantedListener.onLocationPermissionGranted();
        } else {
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    requestMapPermissions(onLocationPermissionGrantedListener);
                }
            }, 500);
        }
    }

    public void openLocationSetting(Context context) {
        try {
            // 核心Intent：跳转到系统定位服务设置专属页面
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            // 检查是否有应用能处理该Intent（避免极少数系统异常）
            if (intent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(intent);
            } else {
                Toast.makeText(context, "无法打开定位设置", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "openLocationSetting: " + e);
            Toast.makeText(context, "打开定位设置失败", Toast.LENGTH_SHORT).show();
        }
    }

    public static boolean isLocationServiceOpened(Context context) {
        try {
            // 获取系统定位服务管理器
            LocationManager locationManager = (LocationManager)
                    context.getSystemService(Context.LOCATION_SERVICE);

            // 检查GPS定位是否开启
            boolean isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            // 检查网络定位是否开启
            boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            // 两者满足其一，即定位服务已开启
            return isGpsEnabled || isNetworkEnabled;
        } catch (Exception e) {
            Log.e(TAG, "isLocationServiceOpened: " + e);
            return false;
        }
    }

    public void CreateMap(onMapLoadFinishedListener onMapLoadFinishedListener) {
        mapView.onCreate(null);
        aMap = mapView.getMap();
        if (aMap == null) {
            Log.e(TAG, "获取AMap实例失败");
            // 延迟500毫秒重新加载地图
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    loadFailedAndRetry(onMapLoadFinishedListener); // 触发失败重试
                }
            }, 500);

            return;
        }
        mapView.getMap().setOnMapLoadedListener(new AMap.OnMapLoadedListener() {
            @Override
            public void onMapLoaded() {
                Log.i(TAG, "地图加载成功");
                onMapLoadFinishedListener.onMapLoadFinished();
            }
        });
    }

    private void loadFailedAndRetry(onMapLoadFinishedListener onMapLoadFinishedListener) {
        Log.i(TAG, "开始重新加载地图");
        // 重置MapView状态（关键：先销毁再重新初始化，避免状态残留）
        if (mapView != null) {
            mapView.onDestroy(); // 销毁原有MapView
        }
        // 重新初始化地图
        initMap(context);
        CreateMap(onMapLoadFinishedListener);
    }

    public void doLocation() {
        MyLocationStyle myLocationStyle;
        myLocationStyle = new MyLocationStyle();//初始化定位蓝点样式类myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE);//连续定位、且将视角移动到地图中心点，定位点依照设备方向旋转，并且会跟随设备移动。（1秒1次定位）如果不设置myLocationType，默认也会执行此种模式。
        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER);
        myLocationStyle.interval(2000); //设置连续定位模式下的定位间隔，只在连续定位模式下生效，单次定位模式下不会生效。单位为毫秒。

        aMap.setMyLocationStyle(myLocationStyle);//设置定位蓝点的Style
        aMap.getUiSettings().setMyLocationButtonEnabled(false);// 设置默认定位按钮是否显示，非必需设置。
        aMap.setMyLocationEnabled(true);// 设置为true表示启动显示定位蓝点，false表示隐藏定位蓝点并不进行定位，默认是false。
    }

    public interface onMapLoadFinishedListener {
        void onMapLoadFinished();
    }

    public interface onLocationPermissionGrantedListener {
        void onLocationPermissionGranted();
    }

    public void addMarker(LatLng latLng, String title) {
        if (title.isEmpty()) {
            title = "marker";
        }
        aMap.addMarker(new MarkerOptions().position(latLng).title(title));
    }

    public void addMarker(LatLonPoint latLng, String title) {
        if (title.isEmpty()) {
            title = "marker";
        }
        aMap.addMarker(new MarkerOptions().position(new LatLng(latLng.getLatitude(), latLng.getLongitude())).title(title));
    }


    public void addPolyline(LatLng start, LatLng end) {
        aMap.addPolyline(new PolylineOptions().add(start, end).width(10).color(Color.RED));
    }

    public void addPolyline(LatLonPoint startP, LatLonPoint endP) {
        LatLng start = new LatLng(startP.getLatitude(), startP.getLongitude());
        LatLng end = new LatLng(endP.getLatitude(), endP.getLongitude());
        aMap.addPolyline(new PolylineOptions().add(start, end).width(10).color(Color.RED));
    }

    public void addPolyline(List<LatLonPoint> points) {
        for (int i = 0; i < points.size() - 1; i++) {
            LatLng start = new LatLng(points.get(i).getLatitude(), points.get(i).getLongitude());
            LatLng end = new LatLng(points.get(i + 1).getLatitude(), points.get(i + 1).getLongitude());
            aMap.addPolyline(new PolylineOptions().add(start, end).width(10).color(Color.RED));
        }
    }

    public void getRoute(LatLng start, LatLng end, RouteSearch.OnRouteSearchListener onRouteSearchListener) {
        try {
            RouteSearch routeSearch = new RouteSearch(context);
            routeSearch.setRouteSearchListener(onRouteSearchListener);
            RouteSearch.FromAndTo fromAndTo = new RouteSearch.FromAndTo(
                    new LatLonPoint(start.latitude, start.longitude),
                    new LatLonPoint(end.latitude, end.longitude)
            );
            Log.i(TAG, String.format("开始规划路径：%s -> %s", start, end));
            RouteSearch.DriveRouteQuery query = new RouteSearch.DriveRouteQuery(fromAndTo, routeSearchStrategy.getStrategy(), null, null, "");
            routeSearch.calculateDriveRouteAsyn(query);
        } catch (AMapException e) {
            throw new RuntimeException(e);
        }
    }


    public void addRoute(LatLng startPoint, LatLng endPoint, Integer color, Integer width) {
        RouteSearch.OnRouteSearchListener onRouteSearchListener = new RouteSearch.OnRouteSearchListener() {
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

                    DrivingRouteOverlay driveRouteOverlay = new DrivingRouteOverlay(
                            context, aMap, drivePath, null);

                    if (width != null) {
                        driveRouteOverlay.setRouteWidth(width);
                    } else {
                        driveRouteOverlay.setRouteWidth(7);
                    }

                    if (color != null) {
                        driveRouteOverlay.setIsColorfulline(true);
                        driveRouteOverlay.addToMap(color);
                    } else {
                        driveRouteOverlay.addToMap();
                    }

                    int dis = (int) drivePath.getDistance();
                    int dur = (int) drivePath.getDuration();
                    String des = MapUtil.getFriendlyTime(dur) + "(" + MapUtil.getFriendlyLength(dis) + ")";
                    Log.d(TAG, des);
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
        };
        getRoute(startPoint, endPoint, onRouteSearchListener);
    }

    public void addRoute(LatLng startPoint, LatLng endPoint, Integer color, Integer width, RouteSearch.OnRouteSearchListener onRouteSearchListener) {
        getRoute(startPoint, endPoint, new RouteSearch.OnRouteSearchListener() {
            @Override
            public void onBusRouteSearched(BusRouteResult busRouteResult, int i) {
                onRouteSearchListener.onBusRouteSearched(busRouteResult, i);
            }

            @Override
            public void onDriveRouteSearched(DriveRouteResult driveRouteResult, int i) {
                onRouteSearchListener.onDriveRouteSearched(driveRouteResult, i);
                if (i == 1000) {
                    Log.i(TAG, "驾车路径规划成功");

                    DrivePath drivePath = driveRouteResult.getPaths().get(0);
                    if (drivePath == null) {
                        return;
                    }

                    DrivingRouteOverlay driveRouteOverlay = new DrivingRouteOverlay(
                            context, aMap, drivePath, null);

                    if (width != null) {
                        driveRouteOverlay.setRouteWidth(width);
                    } else {
                        driveRouteOverlay.setRouteWidth(7);
                    }

                    if (color != null) {
                        driveRouteOverlay.setIsColorfulline(true);
                        driveRouteOverlay.addToMap(color);
                    } else {
                        driveRouteOverlay.addToMap();
                    }

                    int dis = (int) drivePath.getDistance();
                    int dur = (int) drivePath.getDuration();
                    String des = MapUtil.getFriendlyTime(dur) + "(" + MapUtil.getFriendlyLength(dis) + ")";
                    Log.d(TAG, des);
                } else {
                    Log.e(TAG, "驾车路径规划失败,错误码：" + i);
                }
            }

            @Override
            public void onWalkRouteSearched(WalkRouteResult walkRouteResult, int i) {
                onRouteSearchListener.onWalkRouteSearched(walkRouteResult, i);
            }

            @Override
            public void onRideRouteSearched(RideRouteResult rideRouteResult, int i) {
                onRouteSearchListener.onRideRouteSearched(rideRouteResult, i);
            }
        });
    }

    public void addRoute(LatLonPoint startPoint, LatLonPoint endPoint, Integer color, Integer width) {
        LatLng start = new LatLng(startPoint.getLatitude(), startPoint.getLongitude());
        LatLng end = new LatLng(endPoint.getLatitude(), endPoint.getLongitude());
        addRoute(start, end, color, width);
    }

    public void addRoute(LatLonPoint startPoint, LatLonPoint endPoint, Integer color, Integer width, RouteSearch.OnRouteSearchListener onRouteSearchListener) {
        LatLng start = new LatLng(startPoint.getLatitude(), startPoint.getLongitude());
        LatLng end = new LatLng(endPoint.getLatitude(), endPoint.getLongitude());
        addRoute(start, end, color, width, onRouteSearchListener);
    }

    public void moveCamera(LatLng latLng) {
        aMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
    }

    public void moveCamera(LatLonPoint latLonPoint) {
        aMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(latLonPoint.getLatitude(), latLonPoint.getLongitude())));
    }

    public void moveCamera(LatLng latLng, int zoom) {
        aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
    }

    public void moveCamera(LatLonPoint latLonPoint, int zoom) {
        moveCamera(new LatLng(latLonPoint.getLatitude(), latLonPoint.getLongitude()), zoom);
    }

    public RouteSearchStrategy getRouteSearchStrategy() {
        return routeSearchStrategy;
    }

    public void clear() {
        aMap.clear();
    }

    public AMap getAMap() {
        return aMap;
    }


}
