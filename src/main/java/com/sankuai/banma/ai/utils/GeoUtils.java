package com.sankuai.banma.ai.utils;

import com.google.common.collect.Lists;
import com.meituan.sankuai.scope.algorithm.utils.GeographicUtils;
import com.sankuai.meituan.banma.shiparea.helper.PolygonHelper;
import com.sankuai.meituan.banma.shiparea.util.GeometryUtil;
import com.vividsolutions.jts.algorithm.ConvexHull;
import com.vividsolutions.jts.geom.*;
import org.apache.commons.collections.CollectionUtils;
import org.opensphere.geometry.algorithm.ConcaveHull;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zhangrunfeng on 2019-08-21
 */
public class GeoUtils {

    public static final double DEFAULT_DIST_ERROR_VAL = -1.0;

    /**
     * 返回Polygon的JSON字符串
     * @param polygon JTS Polygon
     * @return Polygon的JSON字符串
     */
    public static String getPolygonJSON(Polygon polygon) {
        return PolygonHelper.toPolygonJson(polygon);
    }

    /**
     * 返回Polygon对应的AMAP格式字符串
     * @param polygon JTS Polygon
     * @return Polygon的AMAP格式字符串
     */
    public static String getPolygonAMAP(Polygon polygon) {
        int polygonCoorLen = polygon.getCoordinates().length;
        StringBuilder sb = new StringBuilder("[");
        for (int idx = 0 ; idx < polygonCoorLen - 1; idx+=1) {
            Coordinate c = polygon.getCoordinates()[idx];
            sb.append("[").append(c.y).append(", ").append(c.x).append("]");
            sb.append(",");
        }
        sb.append("[").append(polygon.getCoordinates()[polygonCoorLen-1].y).append(", ").append(polygon.getCoordinates()[polygonCoorLen-1].x).append("]");
        sb.append("]");
        return sb.toString();
    }

    /**
     * 获取指定几何图形的实际面积
     * @param geometry: 几何图形
     * @return 几何图形的面积
     */
    public static double getArea(Geometry geometry) {
        return PolygonHelper.getArea(geometry);
    }

    /**
     * 计算两点之间的距离
     * @param lat1_ori 点1的纬度
     * @param lng1_ori 点1的经度
     * @param lat2_ori 点2的纬度
     * @param lng2_ori 点2的经度
     * @return 两点之间的距离，以米(m)为单位
     */
    public static double getPointDist(double lat1_ori, double lng1_ori, double lat2_ori, double lng2_ori) {
        double dtor = Math.PI / 180.0;
        double threshold = 1000.0;

        double lat1 = lat1_ori;
        if (lat1 > threshold) {
            lat1 /= 1000000.0;
        }
        double lng1 = lng1_ori;
        if (lng1 > threshold) {
            lng1 /= 1000000.0;
        }
        double lat2 = lat2_ori;
        if (lat2 > threshold) {
            lat2 /= 1000000.0;
        }
        double lng2 = lng2_ori;
        if (lng2 > threshold) {
            lng2 /= 1000000.0;
        }

        double lt = Math.sqrt(Math.pow(Math.sin(((lat1 - lat2) * dtor / 2.0)), 2.0) +
                Math.cos(dtor * lat1) * Math.cos(dtor * lat2) * (Math.pow(Math.sin((lng1 - lng2) * dtor / 2.0), 2.0)));

        return 2.0 * Math.asin(lt) * 6378137;
    }

    /**
     * 计算两点之间的距离
     * @param p1 JTS Point (lat, lng)
     * @param p2 JTS Point (lat, lng)
     * @return 两点之间的距离，以米(m)为单位
     */
    public static double getPointDist(Point p1, Point p2) {
        if (p1 == null || p2 == null) {
            return DEFAULT_DIST_ERROR_VAL;
        }
        return getPointDist(p1.getX(), p1.getY(), p2.getX(), p2.getY());
    }

    /**
     * 计算点到多边形顶点之间的最短距离，如果点在多边形内部则返回值为0.0
     * @param p JTS Point
     * @param polygon JTS Polygon
     * @return 点到多边形顶点之间的最近距离
     */
    public static double getPointPolygonMinDist(Point p, Polygon polygon) {
        if (p == null || polygon == null) {
            return DEFAULT_DIST_ERROR_VAL;
        }

        if (polygon.contains(p)){
            return 0.0;
        }

        double minDist = Double.MAX_VALUE;
        for (Coordinate c : polygon.getCoordinates()) {
            Point p2 = GeometryUtil.createPoint(c);
            double currDist = getPointDist(p, p2);
            if (currDist < minDist) {
                minDist = currDist;
            }
        }
        return minDist;
    }

    /**
     * 计算点到多边形顶点的最远直线距离，如果点在多边形内部则返回值为0.0
     * @param p JTS Point
     * @param polygon JTS Polygon
     * @return 点到多边形顶点之间的最远距离
     */
    public static double getPointPolygonMaxDist(Point p, Polygon polygon) {
        if (p == null || polygon == null) {
            return DEFAULT_DIST_ERROR_VAL;
        }

        if (polygon.contains(p)){
            return 0.0;
        }

        double maxDist = -1.0;
        for (Coordinate c : polygon.getCoordinates()) {
            Point p2 = GeometryUtil.createPoint(c);
            double currDist = getPointDist(p, p2);
            if (currDist > maxDist) {
                maxDist = currDist;
            }
        }
        return maxDist;
    }

    /**
     * 计算Polygon列表的外包凸多边形
     * @param polygonList Polygon List
     * @return 外包凸多边形 JTS Polygon
     */
    public static Polygon getPolygonListConvexHull(List<Polygon> polygonList) {
        GeometryFactory geometryFactory = new GeometryFactory();

        GeometryCollection geometryCollection = (GeometryCollection) geometryFactory.buildGeometry(polygonList);
        ConvexHull convexHull = new ConvexHull(geometryCollection.union());

        return (Polygon) convexHull.getConvexHull();
    }

    /**
     * 计算Polygon列表的外包凹多边形
     * @param polygonList Polygon List
     * @return 外包凹多边形 JTS Polygon
     */
    public static Polygon getPolygonListConcaveHull(List<Polygon> polygonList) {
        GeometryFactory geometryFactory = new GeometryFactory();

        GeometryCollection geometryCollection = (GeometryCollection) geometryFactory.buildGeometry(polygonList);
        ConcaveHull concaveHull = new ConcaveHull(geometryCollection, 0.005);

        return (Polygon) concaveHull.getConcaveHull();
    }

    /**
     * 将polygonList转化为AMap字符串
     * @param polygonList Polygon List
     * @return polygonList对应的AMap字符串
     */
    public static String getPolygonListAMap(List<Polygon> polygonList) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int idx = 0; idx < polygonList.size()-1; idx+=1) {
            sb.append(GeoUtils.getPolygonAMAP(polygonList.get(idx)));
            sb.append(",");
        }
        sb.append(GeoUtils.getPolygonAMAP(polygonList.get(polygonList.size()-1)));
        sb.append("]");
        return sb.toString();
    }
}
