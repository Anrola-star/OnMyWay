package com.anrola.onmyway.Fragments;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.anrola.onmyway.R;
import com.amap.api.maps.*;
import com.anrola.onmyway.Utils.AMapManager;


public class NavFragment extends Fragment {

    private Context context;
    private static final String TAG = "NavFragment";

    private AMapManager amapManager;


    private static class viewHolder {
        private static MapView mapView;
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
        initMap(view, savedInstanceState);

        return view;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        viewHolder.mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        viewHolder.mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        viewHolder.mapView.onLowMemory();
    }

    @Override
    public void onPause() {
        super.onPause();

        if (viewHolder.mapView != null) {
            viewHolder.mapView.onPause();
        }
    }

    // 配套：Fragment恢复时的方法（onResume，与onPause对应）
    @Override
    public void onResume() {
        super.onResume();

        if (viewHolder.mapView != null) {
            viewHolder.mapView.onResume();
        }
    }

    private void intiView(View view) {
        context = view.getContext();
    }



    private void initMap(View view, Bundle savedInstanceState) {

        viewHolder.mapView = view.findViewById(R.id.fn_map);
        amapManager = new AMapManager(context, viewHolder.mapView);

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
            }
        });
    }
}