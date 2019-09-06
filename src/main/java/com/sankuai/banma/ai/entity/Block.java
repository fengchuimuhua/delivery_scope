package com.sankuai.banma.ai.entity;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Maps;
import com.sankuai.banma.ai.utils.GeoUtils;
import com.sankuai.meituan.banma.shiparea.helper.PolygonHelper;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

import java.util.Map;

/**
 * Created by zhangrunfeng on 2019-09-02
 */
public class Block {
    // BLOCK ID
    public long blockId;
    // ADMIN CODE，注意这里与cityID不一致
    public long adminCode;
    // ADMIN NAME
    public String adminName;
    // BLOCK 绿化比例
    public double greenPercent;
    // BLOCK 水体覆盖比例
    public double waterPercent;
    // BLOCK 对应的 Polygon
    public Polygon polygon;
    // BLOCK 的重心点
    public Point centroidPoint;
    // BLOCK 面积
    public double shapeArea;
    // BLOCK 周长
    public double shapeLen;

    // BLOCK 单量
    public int orderNum;
    // BLOCK 打分
    public double score;

    public Block(long blockId, long adminCode, String adminName, double greenPercent, double waterPercent, Polygon polygon, Point centroidPoint, double shapeArea, double shapeLen, int orderNum) {
        this.blockId = blockId;
        this.adminCode = adminCode;
        this.adminName = adminName;
        this.greenPercent = greenPercent;
        this.waterPercent = waterPercent;
        this.polygon = polygon;
        this.centroidPoint = centroidPoint;
        this.shapeArea = shapeArea;
        this.shapeLen = shapeLen;
        this.orderNum = orderNum;
    }

    public Block(long blockId, long adminCode, String adminName, double greenPercent, double waterPercent, String scopeStr, Point centroidPoint, double shapeArea, double shapeLen, int orderNum) {
        this.blockId = blockId;
        this.adminCode = adminCode;
        this.adminName = adminName;
        this.greenPercent = greenPercent;
        this.waterPercent = waterPercent;
        this.polygon = PolygonHelper.createPolygonFromArea(scopeStr);
        this.centroidPoint = centroidPoint;
        this.shapeArea = shapeArea;
        this.shapeLen = shapeLen;
        this.orderNum = orderNum;
    }

    public String toJSONString() {
        Map<String, Object> infoMap = Maps.newHashMap();
        infoMap.put("blockid", this.blockId);
        infoMap.put("admincode", this.adminCode);
        infoMap.put("adminname", this.adminName);
        infoMap.put("greenpercent", this.greenPercent);
        infoMap.put("waterpercent", this.waterPercent);
        infoMap.put("polygon", GeoUtils.getPolygonAMAP(this.polygon));
        infoMap.put("area", this.shapeArea);
        infoMap.put("length", this.shapeLen);
        infoMap.put("ordernum", this.orderNum);
        infoMap.put("score", this.score);
        return JSON.toJSONString(infoMap);
    }

    public String toJSONString(double dist) {
        Map<String, Object> infoMap = Maps.newHashMap();
        infoMap.put("blockid", this.blockId);
        infoMap.put("admincode", this.adminCode);
        infoMap.put("adminname", this.adminName);
        infoMap.put("greenpercent", this.greenPercent);
        infoMap.put("waterpercent", this.waterPercent);
        infoMap.put("polygon", GeoUtils.getPolygonAMAP(this.polygon));
        infoMap.put("area", this.shapeArea);
        infoMap.put("length", this.shapeLen);
        infoMap.put("ordernum", this.orderNum);
        infoMap.put("score", this.score);
        infoMap.put("dist", dist);
        return JSON.toJSONString(infoMap);
    }
}
