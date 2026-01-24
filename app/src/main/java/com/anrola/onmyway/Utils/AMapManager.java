package com.anrola.onmyway.Utils;

import static androidx.core.content.ContextCompat.checkSelfPermission;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.amap.api.maps.AMap;
import com.amap.api.maps.MapView;
import com.amap.api.maps.MapsInitializer;
import com.amap.api.maps.model.MyLocationStyle;

import android.Manifest;

import com.anrola.onmyway.R;

import java.util.ArrayList;

public class AMapManager {
    private Context context;
    private MapView mapView;
    private AMap aMap;
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

    public void requestMapPermissions(onLocationPermissionGrantedListener  onLocationPermissionGrantedListener) {
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

    public void CreateMap(onMapLoadFinishedListener  onMapLoadFinishedListener) {
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

    private void loadFailedAndRetry(onMapLoadFinishedListener  onMapLoadFinishedListener) {
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
        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE);
        myLocationStyle.interval(2000); //设置连续定位模式下的定位间隔，只在连续定位模式下生效，单次定位模式下不会生效。单位为毫秒。

        aMap.setMyLocationStyle(myLocationStyle);//设置定位蓝点的Style
        aMap.getUiSettings().setMyLocationButtonEnabled(true);
        aMap.setMyLocationEnabled(true);// 设置为true表示启动显示定位蓝点，false表示隐藏定位蓝点并不进行定位，默认是false。
    }

    public interface onMapLoadFinishedListener {
        void onMapLoadFinished();
    }

    public interface onLocationPermissionGrantedListener {
        void onLocationPermissionGranted();
    }

}
