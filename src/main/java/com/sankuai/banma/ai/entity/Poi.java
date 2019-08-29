package com.sankuai.banma.ai.entity;

import com.vividsolutions.jts.geom.Point;

/**
 * Created by zhangrunfeng on 2019-08-21
 */
public class Poi {
    // 商家ID
    private long poiId;
    // 商家名称
    private String poiName;
    // 商家经度
    private double lng;
    // 商家纬度
    private double lat;
    // 商家坐标点
    private Point poiPoint;

    public Poi(long poiId, String poiName, double lng, double lat, Point poiPoint) {
        this.poiId = poiId;
        this.poiName = poiName;
        this.lng = lng;
        this.lat = lat;
        this.poiPoint = poiPoint;
    }

    public long getPoiId() {
        return poiId;
    }

    public void setPoiId(long poiId) {
        this.poiId = poiId;
    }

    public String getPoiName() {
        return poiName;
    }

    public void setPoiName(String poiName) {
        this.poiName = poiName;
    }

    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public Point getPoiPoint() {
        return poiPoint;
    }

    public void setPoiPoint(Point poiPoint) {
        this.poiPoint = poiPoint;
    }
}
