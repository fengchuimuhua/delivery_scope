package com.sankuai.banma.ai.entity;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sankuai.banma.ai.utils.GeoUtils;
import com.sankuai.meituan.banma.shiparea.helper.PolygonHelper;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Created by zhangrunfeng on 2019-08-21
 */
public class DataCollection {

    // poiID 到 Poi 的映射
    public Map<Long, Poi> pid2PoiMap;

    // aoiID 到 Aoi 的映射
    public Map<Long, Aoi> aid2AoiMap;
    // poiID 对应的候选的AOI列表
    public Map<Long, List<Long>> pid2AoiListMap;

    // blockID 到 Block 的映射
    public Map<Long, Block> bid2BlockMap;
    // poiID 对应的候选的Block列表
    public Map<Long, List<Long>> pid2BlockListMap;

    // 用于生成最终集合图形的Factory
    private GeometryFactory geometryFactory;
    // Well Known Text 格式文件读取器
    private WKTReader wktReader;

    // aoi 数据文件存储位置
    private String aoiDataPath;
    // block 数据文件存储位置
    private String blockDataPath;

    // 构建对象时即完成数据预处理
    public DataCollection(String aoiDataPath, String blockDataPath) {
        this.aoiDataPath = aoiDataPath;
        this.blockDataPath = blockDataPath;
        this.geometryFactory = new GeometryFactory();
        this.wktReader = new WKTReader(this.geometryFactory);
        prepareAoiData();
        prepareBlockData();
    }

    /**
     * 读取AOI文件并将相关数据存储至类成员变量中
     */
    private void prepareAoiData() {
        if (pid2PoiMap == null) {
            pid2PoiMap = Maps.newHashMap();
        }
        if (aid2AoiMap == null) {
            aid2AoiMap = Maps.newHashMap();
        }
        if (pid2AoiListMap == null) {
            pid2AoiListMap = Maps.newHashMap();
        }

        try{
            BufferedReader br = new BufferedReader(new FileReader(aoiDataPath));
            String line;
            while ((line = br.readLine()) != null) {
                String[] info = line.split("\t");

                // AOI信息抽取
                long aoiId = Long.parseLong(info[0]);
                String polygonStr = info[1];
                String centerPointStr = info[2];
                String aoiAddr = info[3];
                String aoiName = info[4];
                int aoiOrderNum = Integer.parseInt(info[5]);

                // POI信息抽取
                long poiId = Long.parseLong(info[6]);
                String poiName = info[7];
                double poiLng = Double.parseDouble(info[8]) / Math.pow(10, 6);
                double poiLat = Double.parseDouble(info[9]) / Math.pow(10, 6);

                // 将商家信息放入到POI映射中
                if (!pid2PoiMap.containsKey(poiId)) {
                    Point poiPoint = geometryFactory.createPoint(new Coordinate(poiLat, poiLng));
                    Poi poi = new Poi(poiId, poiName, poiLng, poiLat, poiPoint);
                    pid2PoiMap.put(poiId, poi);
                }

                // 将AOI信息放入到AOI映射中
                if (!aid2AoiMap.containsKey(aoiId)) {

                    Polygon aoiPolygon = (Polygon) wktReader.read(processWKTPolygonInput(polygonStr));
                    Point aoiCentroid = (Point) wktReader.read(processWKTPointInput(centerPointStr));
                    double aoiArea = GeoUtils.getArea(aoiPolygon);

                    Aoi aoi = new Aoi(aoiId, aoiAddr, aoiName, aoiOrderNum, aoiPolygon, aoiCentroid, aoiArea);

                    aid2AoiMap.put(aoiId, aoi);
                }

                // 构建商家到潜在AOI的映射
                if (!pid2AoiListMap.containsKey(poiId)) {
                    pid2AoiListMap.put(poiId, new ArrayList<>());
                }
                if (!pid2AoiListMap.get(poiId).contains(aoiId)) {
                    pid2AoiListMap.get(poiId).add(aoiId);
                }
            }

            System.out.println("poi number : " + pid2PoiMap.keySet().size());
            System.out.println("aoi number : " + aid2AoiMap.keySet().size());
            for (Map.Entry<Long, List<Long>> entry : pid2AoiListMap.entrySet()) {
                System.out.println("-- poi id : " + entry.getKey() + "; candidate aoi number : " + entry.getValue().size());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 读取Block文件并将相关数据存储至类成员变量中
     */
    private void prepareBlockData() {
        if (bid2BlockMap == null) {
            bid2BlockMap = Maps.newHashMap();
        }
        if (pid2BlockListMap == null) {
            pid2BlockListMap = Maps.newHashMap();
        }

        try{
            BufferedReader br = new BufferedReader(new FileReader(blockDataPath));
            String line;
            while((line = br.readLine()) != null) {
                try{
                    String[] infoArr = line.split("\t");

                    long poiId = Long.parseLong(infoArr[0]);
                    long blockId = Long.parseLong(infoArr[1]);
                    long adminCode = Long.parseLong(infoArr[2]);
                    String adminName = infoArr[3];
                    double greenPercent = Double.parseDouble(infoArr[4]);
                    double waterPercent = Double.parseDouble(infoArr[5]);
                    String scopeStr = infoArr[6];
                    double shapeArea = Double.parseDouble(infoArr[7]);
                    double shapeLen = Double.parseDouble(infoArr[8]);
                    double dist = Double.parseDouble(infoArr[9]);
                    int orderNum = Integer.parseInt(infoArr[10]);

                    if (!bid2BlockMap.containsKey(blockId)) {
                        Polygon blockPolygon = PolygonHelper.createPolygonFromArea(scopeStr);
                        Point centroidPoint = blockPolygon.getCentroid();
                        Block block = new Block(blockId, adminCode, adminName, greenPercent, waterPercent, blockPolygon, centroidPoint, shapeArea, shapeLen, orderNum);
                        bid2BlockMap.put(blockId, block);
                    }

                    if (!pid2BlockListMap.containsKey(poiId)) {
                        pid2BlockListMap.put(poiId, Lists.newArrayList());
                    }

                    if (!pid2BlockListMap.get(poiId).contains(blockId)) {
                        pid2BlockListMap.get(poiId).add(blockId);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 交换WKT格式POINT的经纬度
     * @param str WKT格式的GEO字符串
     * @return 交换经纬度之后的WKT格式
     * @throws ParseException 可能抛出WKT格式解析异常
     */
    private String processWKTPointInput(String str) throws ParseException {
        Point p = (Point) wktReader.read(str);
        return "POINT (" + p.getY() + " " + p.getX() + ")";
    }

    /**
     * 当前AOI的存储格式为WKT格式，通过WKTReader读取后的Polygon与线上处理的结果相反
     * @param str WKT格式的GEO字符串
     * @return 交换经纬度之后的WKT格式
     * @throws ParseException 可能抛出WKT格式解析异常
     */
    private String processWKTPolygonInput(String str) throws ParseException {
        Polygon p = (Polygon)wktReader.read(str);
        Coordinate[] points = p.getCoordinates();
        StringBuilder sb = new StringBuilder();
        sb.append("POLYGON ((");
        for (int idx = 0; idx < points.length-1; idx++) {
            sb.append(points[idx].y);
            sb.append(" ");
            sb.append(points[idx].x);
            sb.append(",");
        }
        sb.append(points[points.length-1].y);
        sb.append(" ");
        sb.append(points[points.length-1].x);
        sb.append("))");
        return sb.toString();
    }

}
