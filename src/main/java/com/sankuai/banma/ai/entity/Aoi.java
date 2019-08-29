package com.sankuai.banma.ai.entity;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Maps;
import com.sankuai.banma.ai.utils.GeoUtils;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

import java.util.Map;

/**
 * Created by zhangrunfeng on 2019-08-21
 */
public class Aoi {
    // AOI ID
    private long aoiId;
    // AOI地址
    private String addr;
    // AOI名称
    public String name;
    // AOI上的单量
    private int orderNum;
    // AOI多边形，JTS Polygon类型
    private Polygon polygon;
    // AOI重心点，JTS Point类型
    private Point centroidPoint;
    // AOI打分，用来对AOI排序并进行选择
    private double score; // 用于aoi和poi的打分, 临时存储
    // AOI 面积
    private double area; // 面积

    public Aoi(long aoiId, String addr, String name,
               int orderNum, Polygon polygon, Point centroidPoint, double area) {
        this.aoiId = aoiId;
        this.addr = addr;
        this.name = name;
        this.orderNum = orderNum;
        this.polygon = polygon;
        this.centroidPoint = centroidPoint;
        this.area = area;
    }

    public long getAoiId() {
        return aoiId;
    }

    public void setAoiId(long aoiId) {
        this.aoiId = aoiId;
    }

    public String getAddr() {
        return addr;
    }

    public void setAddr(String addr) {
        this.addr = addr;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getOrderNum() {
        return orderNum;
    }

    public void setOrderNum(int orderNum) {
        this.orderNum = orderNum;
    }

    public Polygon getPolygon() {
        return polygon;
    }

    public void setPolygon(Polygon polygon) {
        this.polygon = polygon;
    }

    public Point getCentroidPoint() {
        return centroidPoint;
    }

    public void setCentroidPoint(Point centroidPoint) {
        this.centroidPoint = centroidPoint;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public double getArea() {
        return area;
    }

    public void setArea(double area) {
        this.area = area;
    }

    public String toJSONString() {
        Map<String, Object> infoMap = Maps.newHashMap();
        infoMap.put("name", this.name);
        infoMap.put("addr", this.addr);
        infoMap.put("score", this.score);
        infoMap.put("area", this.area);
        infoMap.put("polygon", GeoUtils.getPolygonAMAP(this.polygon));
        infoMap.put("ordernum", this.orderNum);
        return JSON.toJSONString(infoMap);
    }

    public String toJSONString(double dist) {
        Map<String, Object> infoMap = Maps.newHashMap();
        infoMap.put("name", this.name);
        infoMap.put("addr", this.addr);
        infoMap.put("score", this.score);
        infoMap.put("area", this.area);
        infoMap.put("polygon", GeoUtils.getPolygonAMAP(this.polygon));
        infoMap.put("ordernum", this.orderNum);
        infoMap.put("dist", dist);
        return JSON.toJSONString(infoMap);
    }
}
