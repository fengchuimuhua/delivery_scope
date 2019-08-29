package com.sankuai.banma.ai.algo;

import ch.hsr.geohash.GeoHash;
import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.sankuai.banma.ai.entity.Aoi;
import com.sankuai.banma.ai.entity.Poi;
import com.sankuai.banma.ai.entity.PoiAoiData;
import com.sankuai.banma.ai.utils.GeoUtils;
import com.sankuai.meituan.banma.shiparea.helper.PolygonHelper;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.xlvecle.GeohashPolyKt;

import java.io.*;
import java.util.*;

/**
 * Created by zhangrunfeng on 2019-08-21
 */
public class AoiSelectAlgo {
    // POI与AOI数据
    public PoiAoiData data;
    // 计算结果：poi应该包含的aoiID List
    public Map<Long, List<Long>> poiScopeAoiList;
    // 计算结果：poi包含的aoi最终组成的凹多边形
    public Map<Long, Polygon> poiScopeMap;
    // 商家坐标点到AOI中心点的导航聚类
    public Map<String, Double> distMap;

    private static final double MAX_AREA_THRESHOLD = 5.3 * Math.pow(10, 7);

    public AoiSelectAlgo(String dataPath){
        data = new PoiAoiData(dataPath);
    }

    public void run() {
        compAoiScore();
        selectAoi();
    }

    public void loadDistMap(String distFilePath) {
        distMap = Maps.newHashMap();
        try {
            BufferedReader br = new BufferedReader(new FileReader(distFilePath));
            String line;
            while((line = br.readLine()) != null) {
                String[] strs = line.split(",");
                distMap.put(strs[0] + "_" + strs[1], Double.valueOf(strs[2]));
            }
            br.close();
        } catch (Exception e) {
            System.out.println("Fail to read dist file!");
        }
    }

    /**
     * 对POI对应的candidate AOI打分
     */
    private void compAoiScore() {
        for (Map.Entry<Long, List<Long>> entry : data.pid2AoiListMap.entrySet()) {
            long poiId = entry.getKey();

            List<Long> aoiIdList = entry.getValue();
            for (long aoiId : aoiIdList) {
                double dist = getPoiAoiDist(poiId, aoiId) / 1000.0;

                double aoiScore;
                if (dist <= 3) {
                    aoiScore = data.aid2AoiMap.get(aoiId).getOrderNum() / Math.pow(2, dist);
                } else {
                    aoiScore = data.aid2AoiMap.get(aoiId).getOrderNum() / Math.pow(3, dist);
                }
                data.aid2AoiMap.get(aoiId).setScore(aoiScore);
            }
        }
    }

//    public void compAoiScore(String distFilePath) {
//        Map<String, Double> distMap = Maps.newHashMap();
//        try {
//            BufferedReader br = new BufferedReader(new FileReader(distFilePath));
//            String line;
//            while((line = br.readLine()) != null) {
//                String[] strs = line.split(",");
//                distMap.put(strs[0] + "_" + strs[1], Double.valueOf(strs[2]));
//            }
//            br.close();
//        } catch (Exception e) {
//            System.out.println("Fail to read dist file!");
//        }
//
//        for (Map.Entry<Long, List<Long>> entry : data.pid2AoiListMap.entrySet()) {
//            long poiId = entry.getKey();
//
//            List<Long> aoiIdList = entry.getValue();
//            for (long aoiId : aoiIdList) {
//                double dist;
//                String key = String.valueOf(poiId) + "_" + String.valueOf(aoiId);
//                if (distMap.containsKey(key)) {
//                    dist = distMap.get(key) / 1000.0;
//                } else {
//                    dist = 5 + GeoUtils.getPointDist(data.pid2PoiMap.get(poiId).getPoiPoint(), data.aid2AoiMap.get(aoiId).getCentroidPoint()) / 1000.0;
//                }
//
//                double aoiScore;
//                if (dist <= 3) {
//                    aoiScore = data.aid2AoiMap.get(aoiId).getOrderNum() / Math.pow(2, dist);
//                } else {
//                    aoiScore = data.aid2AoiMap.get(aoiId).getOrderNum() / Math.pow(3, dist);
//                }
//                data.aid2AoiMap.get(aoiId).setScore(aoiScore);
//            }
//        }
//    }

    /**
     * 为每个poi选择合适的应该被范围包含进去的AOI，计算结果放入至成员变量 poiScopeAoiList 中
     */
    public void selectAoi() {
        poiScopeAoiList = Maps.newHashMap();
        poiScopeMap = Maps.newHashMap();

        for (Map.Entry<Long, List<Long>> entry : data.pid2AoiListMap.entrySet()) {
            long poiId = entry.getKey();
            List<Long> aoiIdList = entry.getValue();

            aoiIdList.sort((o1, o2) -> Double.compare(data.aid2AoiMap.get(o2).getScore(), data.aid2AoiMap.get(o1).getScore()));

            List<Long> scopeAoiList = new ArrayList<>();
            List<Polygon> scopePolygonList = new ArrayList<>();

            int initialPolygonNum = 10;

            for (int idx = 0 ; idx < initialPolygonNum; idx+=1) {
                long aoiId = aoiIdList.get(idx);
                scopeAoiList.add(aoiId);
                scopePolygonList.add(data.aid2AoiMap.get(aoiId).getPolygon());
            }

            for (int idx = initialPolygonNum; idx < aoiIdList.size(); idx+=1) {
                long aoiId = aoiIdList.get(idx);
                scopeAoiList.add(aoiId);
                scopePolygonList.add(data.aid2AoiMap.get(aoiId).getPolygon());

                Polygon currConcavePolygon = GeoUtils.getPolygonListConcaveHull(scopePolygonList);
                double currPolygonArea = GeoUtils.getArea(currConcavePolygon);
                if (currPolygonArea > MAX_AREA_THRESHOLD) {
                    break;
                }
            }
            scopeAoiList.remove(scopeAoiList.size()-1);
            scopePolygonList.remove(scopePolygonList.size()-1);

            poiScopeAoiList.put(poiId, scopeAoiList);
            poiScopeMap.put(poiId, GeoUtils.getPolygonListConcaveHull(scopePolygonList));
        }
    }

    /**
     * 将AOI List转为AMap格式字符串
     * @param aoiIdList AOI List
     * @return AMap格式字符串
     */
    private String getPolygonListAMap(List<Long> aoiIdList) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int idx = 0; idx < aoiIdList.size()-1; idx+=1) {
            long aoiId = aoiIdList.get(idx);
            sb.append(GeoUtils.getPolygonAMAP(data.aid2AoiMap.get(aoiId).getPolygon()));
            sb.append(",");
        }
        long lastAoiId = aoiIdList.get(aoiIdList.size()-1);
        sb.append(GeoUtils.getPolygonAMAP(data.aid2AoiMap.get(lastAoiId).getPolygon()));
        sb.append("]");
        return sb.toString();
    }

    private String getPolygonInfoString(long poiId, List<Long> aoiList) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int idx = 0; idx < aoiList.size()-1; idx+=1) {
            long aoiId = aoiList.get(idx);
            double dist = getPoiAoiDist(poiId, aoiId);
            String infoStr = data.aid2AoiMap.get(aoiId).toJSONString(dist);
            sb.append(infoStr).append(",");
        }
        long lastAoiId = aoiList.get(aoiList.size()-1);
        double dist = getPoiAoiDist(poiId, lastAoiId);
        sb.append(data.aid2AoiMap.get(lastAoiId).toJSONString(dist)).append("]");
        return sb.toString();
    }

    /**
     * 获取一定距离范围内不被选中的AOI集合，转化为AMap格式字符串
     * @param poiId 商家ID
     * @param maxDist 最大距离约束
     * @return AMap格式字符串
     */
    private String getNearbyNotInScopePolygonAMap(long poiId, double maxDist) {
        List<Long> aoiIdList = poiScopeAoiList.get(poiId);
        Set<Long> aoiIdSet = Sets.newHashSet(aoiIdList);
        List<Long> allAoiList = data.pid2AoiListMap.get(poiId);

        List<Polygon> notInScopeAoiPolygonList = Lists.newArrayList();
        for (long aoiId : allAoiList) {
            if (aoiIdSet.contains(aoiId)){
                continue;
            }
            Polygon aoiPolygon = data.aid2AoiMap.get(aoiId).getPolygon();
            if (getPoiAoiDist(poiId, aoiId) <= maxDist) {
                notInScopeAoiPolygonList.add(aoiPolygon);
            }
        }
        return GeoUtils.getPolygonListAMap(notInScopeAoiPolygonList);
    }

    private String getNearbyNotInScopePolygonInfo(long poiId, double maxDist) {
        List<Long> aoiIdList = poiScopeAoiList.get(poiId);
        Set<Long> aoiIdSet = Sets.newHashSet(aoiIdList);
        List<Long> allAoiList = data.pid2AoiListMap.get(poiId);

        List<Long> aoiList = Lists.newArrayList();
        for (long aoiId : allAoiList) {
            if (aoiIdSet.contains(aoiId)){
                continue;
            }
            if (getPoiAoiDist(poiId, aoiId) <= maxDist) {
                aoiList.add(aoiId);
            }
        }
        return getPolygonInfoString(poiId, aoiList);
    }

    /**
     * 将AOI选择的结果写入到文件中(JavaScript 文件)
     * @param poiId 商家ID
     * @param dataPath SQL执行结果数据文件
     * @param oldScope 商家当前配送范围，JSON字符串
     * @param cityName 城市名称，用来指定存储文件夹
     * @param maxAoiDistThres 选择AOI时的最远AOI路径
     */
    public void writeJsDataFile(long poiId, String dataPath, String oldScope, String cityName, double maxAoiDistThres) {
        // 用来存放商家坐标点的AMap文件的URL（js文件）
        String poiPointPath = "display/revenue/js/" + cityName + "/poi_point.js";
        // 用来存放新生成的商家配送范围的AMap文件的URL（js文件）
        String newScopePath = "display/revenue/js/" + cityName + "/poi_scope.js";
        // 用来存放商家当前配送范围的AMap文件的URL（js文件）
        String oldScopePath = "display/revenue/js/" + cityName + "/original_scope.js";
        // 用来存放candidate AOIs但最终没有被选中的AOIs AMap格式（js文件）
        String notInScopeAoiPath = "display/revenue/js/" + cityName + "/not_in_scope_aoi.js";
        // 用来存放candidate AOIs最终被选中的AOIs AMap格式（js文件）
        String scopeAoiPath = "display/revenue/js/" + cityName + "/scope_aoi.js";

        try {
            File f = new File("display/revenue/js/" + cityName);
            if (!f.exists()) {
                boolean mkdirs = f.mkdirs();
                if (!mkdirs) return;
            }

            String oldScopeAMap = GeoUtils.getPolygonAMAP(PolygonHelper.createPolygonFromArea(oldScope));
            BufferedWriter bw = new BufferedWriter(new FileWriter(oldScopePath));
            bw.write("var original_scope = " + oldScopeAMap);
            bw.close();

            String newScopeAMap = GeoUtils.getPolygonAMAP(poiScopeMap.get(poiId));
            bw = new BufferedWriter(new FileWriter(newScopePath));
            bw.write("var poi_scope = " + newScopeAMap + ";");
            bw.close();

            String notInScopeAoiAMap = getNearbyNotInScopePolygonAMap(poiId, maxAoiDistThres);
            bw = new BufferedWriter(new FileWriter(notInScopeAoiPath));
            bw.write("var not_in_scope_aois = " + notInScopeAoiAMap + ";");
            bw.close();

            String scopeAoiAMap = getPolygonListAMap(poiScopeAoiList.get(poiId));
            bw = new BufferedWriter(new FileWriter(scopeAoiPath));
            bw.write("var scope_aoi = " + scopeAoiAMap + ";");
            bw.close();

            String poiPointAMap = "[" + data.pid2PoiMap.get(poiId).getLng() + "," + data.pid2PoiMap.get(poiId).getLat() + "]";
            bw = new BufferedWriter(new FileWriter(poiPointPath));
            bw.write("var poi_point = " + poiPointAMap + ";");
            bw.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public void writeJSDataFileWithAoiInfo(long poiId, String dataPath, String oldScope, String cityName, double maxAoiDistThres) {
        // 用来存放商家坐标点的AMap文件的URL（js文件）
        String poiPointPath = "display/revenue/js/" + cityName + "/poi_point.js";
        // 用来存放新生成的商家配送范围的AMap文件的URL（js文件）
        String newScopePath = "display/revenue/js/" + cityName + "/poi_scope.js";
        // 用来存放商家当前配送范围的AMap文件的URL（js文件）
        String oldScopePath = "display/revenue/js/" + cityName + "/original_scope.js";
        // 用来存放candidate AOIs但最终没有被选中的AOI信息 JSON 格式（js文件）
        String notInScopeAoiInfoPath = "display/revenue/js/" + cityName + "/not_in_scope_aoi_info.js";
        // 用来存放candidate AOIs最终被选中的AOI信息 JSON格式（js文件）
        String scopeAoiInfoPath = "display/revenue/js/" + cityName + "/scope_aoi_info.js";
        // 用来存放candidate AOIs但最终没有被选中的AOIs AMap格式（js文件）
        String notInScopeAoiPolygonPath = "display/revenue/js/" + cityName + "/not_in_scope_aoi.js";
        // 用来存放candidate AOIs最终被选中的AOIs AMap格式（js文件）
        String scopeAoiPolygonPath = "display/revenue/js/" + cityName + "/scope_aoi.js";

        try {
            File f = new File("display/revenue/js/" + cityName);
            if (!f.exists()) {
                boolean mkdirs = f.mkdirs();
                if (!mkdirs) return;
            }

            String oldScopeAMap = GeoUtils.getPolygonAMAP(PolygonHelper.createPolygonFromArea(oldScope));
            BufferedWriter bw = new BufferedWriter(new FileWriter(oldScopePath));
            bw.write("var original_scope = " + oldScopeAMap);
            bw.close();

            String newScopeAMap = GeoUtils.getPolygonAMAP(poiScopeMap.get(poiId));
            bw = new BufferedWriter(new FileWriter(newScopePath));
            bw.write("var poi_scope = " + newScopeAMap + ";");
            bw.close();

            String notInScopeAoiInfo = getNearbyNotInScopePolygonInfo(poiId, maxAoiDistThres);
            bw = new BufferedWriter(new FileWriter(notInScopeAoiInfoPath));
            bw.write("var not_in_scope_aoi_info = " + notInScopeAoiInfo + ";");
            bw.close();

            String scopeAoiInfo = getPolygonInfoString(poiId, poiScopeAoiList.get(poiId));
            bw = new BufferedWriter(new FileWriter(scopeAoiInfoPath));
            bw.write("var scope_aoi_info = " + scopeAoiInfo + ";");
            bw.close();

            String notInScopeAoiAMap = getNearbyNotInScopePolygonAMap(poiId, maxAoiDistThres);
            bw = new BufferedWriter(new FileWriter(notInScopeAoiPolygonPath));
            bw.write("var not_in_scope_aois = " + notInScopeAoiAMap + ";");
            bw.close();

            String scopeAoiAMap = getPolygonListAMap(poiScopeAoiList.get(poiId));
            bw = new BufferedWriter(new FileWriter(scopeAoiPolygonPath));
            bw.write("var scope_aoi = " + scopeAoiAMap + ";");
            bw.close();

            String poiPointAMap = "[" + data.pid2PoiMap.get(poiId).getLng() + "," + data.pid2PoiMap.get(poiId).getLat() + "]";
            bw = new BufferedWriter(new FileWriter(poiPointPath));
            bw.write("var poi_point = " + poiPointAMap + ";");
            bw.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 将Geo2Geo的信息写入到文件中，用于查询商家到AOI的骑行导航距离
     * @param filePath 文件路径
     */
    public void writeGeo2GeoFile(String filePath) {
        try{
            BufferedWriter bw = new BufferedWriter(new FileWriter(filePath));

            String tableHeader = "poi_id,aoi_id,poi_geo,aoi_geo,poi_lat,poi_lng,aoi_lat,aoi_lng\n";
            bw.write(tableHeader);

            for (Map.Entry<Long, List<Long>> entry : data.pid2AoiListMap.entrySet()) {
                long poiId = entry.getKey();
                List<Long> aoiList = entry.getValue();

                Poi poi = data.pid2PoiMap.get(poiId);
                String poiGeoHash = GeoHash.geoHashStringWithCharacterPrecision(poi.getLat(), poi.getLng(), 8);

                for (long aoiId : aoiList) {
                    Aoi aoi = data.aid2AoiMap.get(aoiId);
                    String aoiCentroidGeoHash = GeoHash.geoHashStringWithCharacterPrecision(aoi.getCentroidPoint().getX(), aoi.getCentroidPoint().getY(), 8);

                    // 计算如果商家的距离离AOI中心点8km之内的AOI则会被选中
                    if (GeoUtils.getPointDist(poi.getPoiPoint(), aoi.getCentroidPoint()) > 8000.0) {
                        continue;
                    }
                    bw.write(poiId + "," + aoiId + "," + poiGeoHash + "," + aoiCentroidGeoHash + "," + poi.getLat() + "," + poi.getLng() + "," + aoi.getCentroidPoint().getX() + "," + aoi.getCentroidPoint().getY() + "\n");
                }
            }

            bw.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private double getPoiAoiDist(long poiId, long aoiId) {
        if ((!data.pid2PoiMap.containsKey(poiId))
                || (!data.aid2AoiMap.containsKey(aoiId))) {
            return -1.0;
        }

        double dist = GeoUtils.getPointDist(data.pid2PoiMap.get(poiId).getPoiPoint(), data.aid2AoiMap.get(aoiId).getCentroidPoint());
        if (!distMap.isEmpty()) {
            String key = String.valueOf(poiId) + "_" + String.valueOf(aoiId);
            if (distMap.containsKey(key)) {
                dist = distMap.get(key);
            }
        }
        return dist;
    }
}
