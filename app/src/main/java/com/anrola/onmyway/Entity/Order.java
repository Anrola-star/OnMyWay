package com.anrola.onmyway.Entity;

import com.amap.api.maps.model.LatLng;
import com.anrola.onmyway.Utils.DrivingRouteOverlay;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

public class Order implements Serializable {
    private String no;
    private String title;
    private String receiverName;
    private String receiverMobile;
    private String receiverProvince;
    private String receiverCity;
    private String receiverDistrict;
    private String receiverAddress;
    private JSONObject startLocation;
    private JSONObject endLocation;
    private int distance;
    private int amount;
    private String startTime;
    private String requireTime;
    private boolean isAccepted;
    private boolean isPickUp;
    private boolean isFinished;

    public Order() {}

    public Order(String no, String title,
                 String receiverName, String receiverMobile,
                 String receiverProvince, String receiverCity, String receiverDistrict, String receiverAddress,
                 JSONObject startLocation, JSONObject endLocation,
                 int amount,
                 String startTime, String requireTime,
                 boolean isAccepted, boolean isPickup, boolean isFinished) {
        this.no = no;
        this.title = title;
        this.receiverName = receiverName;
        this.receiverMobile = receiverMobile;
        this.receiverProvince = receiverProvince;
        this.receiverCity = receiverCity;
        this.receiverDistrict = receiverDistrict;
        this.receiverAddress = receiverAddress;
        this.startLocation = startLocation;
        this.endLocation = endLocation;

        LatLng startLatLng = new LatLng(0, 0);
        LatLng endLatLng = new LatLng(0, 0);
        try {
            startLatLng = new LatLng(
                    startLocation.getDouble("latitude"),
                    startLocation.getDouble("longitude"));
            endLatLng = new LatLng(
                    endLocation.getDouble("latitude"),
                    endLocation.getDouble("longitude"));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        this.distance = getDistanceByLocation(startLatLng, endLatLng);
        this.amount = amount;
        this.startTime = startTime;
        this.requireTime = requireTime;
        this.isAccepted = isAccepted;
        this.isPickUp = isPickup;
        this.isFinished = isFinished;
    }

    public Order(Order  order){
        this.no = order.no;
        this.title = order.title;
        this.receiverName = order.receiverName;
        this.receiverMobile = order.receiverMobile;
        this.receiverProvince = order.receiverProvince;
        this.receiverCity = order.receiverCity;
        this.receiverDistrict = order.receiverDistrict;
        this.receiverAddress = order.receiverAddress;
        this.startLocation = order.startLocation;
        this.endLocation = order.endLocation;

        LatLng startLatLng = new LatLng(0, 0);
        LatLng endLatLng = new LatLng(0, 0);
        try {
            startLatLng = new LatLng(
                    order.getStartLocation().getDouble("latitude"),
                    order.getStartLocation().getDouble("longitude"));
            endLatLng = new LatLng(
                    order.getEndLocation().getDouble("latitude"),
                    order.getEndLocation().getDouble("longitude"));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        this.distance = getDistanceByLocation(startLatLng, endLatLng);
        this.amount = order.amount;
        this.startTime = order.startTime;
        this.requireTime = order.requireTime;
        this.isAccepted = order.isAccepted;
        this.isPickUp = order.isPickUp;
        this.isFinished = order.isFinished;
    }

    public int getDistanceByLocation(LatLng startLocation, LatLng endLocation){

        return distance = DrivingRouteOverlay.calculateDistance(startLocation, endLocation);
    }

    public String getNo() {
        return no;
    }

    public void setNo(String no) {
        this.no = no;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    public String getReceiverMobile() {
        return receiverMobile;
    }

    public void setReceiverMobile(String receiverMobile) {
        this.receiverMobile = receiverMobile;
    }

    public String getReceiverProvince() {
        return receiverProvince;
    }

    public void setReceiverProvince(String receiverProvince) {
        this.receiverProvince = receiverProvince;
    }

    public String getReceiverCity() {
        return receiverCity;
    }

    public void setReceiverCity(String receiverCity) {
        this.receiverCity = receiverCity;
    }

    public String getReceiverDistrict() {
        return receiverDistrict;
    }

    public void setReceiverDistrict(String receiverDistrict) {
        this.receiverDistrict = receiverDistrict;
    }

    public String getReceiverAddress() {
        return receiverAddress;
    }

    public void setReceiverAddress(String receiverAddress) {
        this.receiverAddress = receiverAddress;
    }

    public JSONObject getStartLocation() {
        return startLocation;
    }

    public void setStartLocation(JSONObject startLocation) {
        this.startLocation = startLocation;
    }

    public JSONObject getEndLocation() {
        return endLocation;
    }

    public void setEndLocation(JSONObject endLocation) {
        this.endLocation = endLocation;
    }

    public int getDistance() {
        return distance;
    }

    public void setDistance(int distance) {
        this.distance = distance;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getRequireTime() {
        return requireTime;
    }

    public void setRequireTime(String requireTime) {
        this.requireTime = requireTime;
    }

    public boolean isAccepted() {
        return isAccepted;
    }

    public void setAccepted(boolean isAccepted) {
        this.isAccepted = isAccepted;
    }

    public boolean isPickup() {
        return isPickUp;
    }

    public void setPickup(boolean isPickup) {
        this.isPickUp = isPickup;
    }

    public boolean isFinished() {
        return isFinished;
    }

    public void setFinished(boolean isFinished) {
        this.isFinished = isFinished;
    }
}
