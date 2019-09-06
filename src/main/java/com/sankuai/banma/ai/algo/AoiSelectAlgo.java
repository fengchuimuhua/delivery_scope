package com.sankuai.banma.ai.algo;

import ch.hsr.geohash.GeoHash;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.sankuai.banma.ai.entity.Aoi;
import com.sankuai.banma.ai.entity.Poi;
import com.sankuai.banma.ai.entity.DataCollection;
import com.sankuai.banma.ai.utils.GeoUtils;
import com.sankuai.meituan.banma.shiparea.helper.PolygonHelper;
import com.sankuai.meituan.banma.shiparea.util.GeometryUtil;
import com.vividsolutions.jts.geom.Polygon;

import java.io.*;
import java.util.*;

/**
 * Created by zhangrunfeng on 2019-08-21
 */
public class AoiSelectAlgo {
    // POI与AOI数据
    public DataCollection data;

    // 计算结果：poi应该包含的aoiID List
    public Map<Long, List<Long>> poiScopeAoiList;
    // 计算结果：poi包含的aoi最终组成的凹多边形
    public Map<Long, Polygon> poiAScopeMap;
    // 商家坐标点到AOI中心点的导航距离映射
    public Map<String, Double> aoiDistMap;

    // 计算结果：poi应该包含的blockID List
    public Map<Long, List<Long>> poiScopeBlockList;
    // 计算结果：poi包含的block最终组成的凹多边形
    public Map<Long, Polygon> poiBScopeMap;
    // 商家坐标点到Block中心点的导航距离映射
    public Map<String, Double> blockDistMap;

    public double MAX_AREA_THRESHOLD = 3.59 * Math.pow(10, 7);

    public AoiSelectAlgo(String aoiDataPath, String blockDataPath){
        data = new DataCollection(aoiDataPath, blockDataPath);
    }

    public void run() {
        compAoiScore();
        selectAoi();
    }

    public void loadAoiDistMap(String distFilePath) {
        aoiDistMap = Maps.newHashMap();
        try {
            BufferedReader br = new BufferedReader(new FileReader(distFilePath));
            String line;
            while((line = br.readLine()) != null) {
                String[] strs = line.split(",");
                aoiDistMap.put(strs[0] + "_" + strs[1], Double.valueOf(strs[2]));
            }
            br.close();
        } catch (Exception e) {
            System.out.println("Fail to read aoi dist file!");
        }
    }

    public void loadBlockDistMap(String distFilePath) {
        blockDistMap = Maps.newHashMap();
        try {
            BufferedReader br = new BufferedReader(new FileReader(distFilePath));
            String line;
            while((line = br.readLine()) != null) {
                String[] strs = line.split(",");
                blockDistMap.put(strs[0] + "_" + strs[1], Double.valueOf(strs[2]));
            }
            br.close();
        } catch (Exception e) {
            System.out.println("Fail to read block dist file!");
        }
    }

    /**
     * 对 Poi 对应的 candidate Aoi 打分
     */
    public void compAoiScore() {
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

    /**
     * 对 Poi 对应的 candidate Block 打分
     */
    public void compBlockScore() {
        for (Map.Entry<Long, List<Long>> entry : data.pid2BlockListMap.entrySet()) {
            long poiId = entry.getKey();

            List<Long> blockIdList = entry.getValue();
            for (long blockId : blockIdList) {
                double dist = getPoiBlockDist(poiId, blockId) / 1000.0;

                double blockScore;
                if (dist <= 3) {
                    blockScore = data.bid2BlockMap.get(blockId).orderNum / Math.pow(2, dist);
                } else {
                    blockScore = data.bid2BlockMap.get(blockId).orderNum / Math.pow(3, dist);
                }
                data.bid2BlockMap.get(blockId).score = blockScore;
            }
        }
    }

    /**
     * 为每个poi选择合适的应该被范围包含进去的AOI，计算结果放入至成员变量 poiScopeAoiList 中
     */
    public void selectAoi() {
        poiScopeAoiList = Maps.newHashMap();
        poiAScopeMap = Maps.newHashMap();

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
            poiAScopeMap.put(poiId, GeoUtils.getPolygonListConcaveHull(scopePolygonList));
        }
    }

    /**
     * 为每个poi选择合适的应该被范围包含进去的Block，计算结果放入成员变量 poiScopeBlockList 中
     */
    public void selectBlock() {
        poiScopeBlockList = Maps.newHashMap();
        poiBScopeMap = Maps.newHashMap();

        for (Map.Entry<Long, List<Long>> entry : data.pid2BlockListMap.entrySet()) {
            long poiId = entry.getKey();
            List<Long> blockIdList = entry.getValue();

            blockIdList.sort((o1, o2) -> Double.compare(data.bid2BlockMap.get(o2).score, data.bid2BlockMap.get(o1).score));

            List<Long> scopeBlockList = Lists.newArrayList();
            List<Polygon> scopePolygonList = Lists.newArrayList();

            int initialPolygonNum = 10;

            for (int idx = 0; (idx < initialPolygonNum) && (idx < blockIdList.size()); idx+=1) {
                long blockId = blockIdList.get(idx);
                scopeBlockList.add(blockId);
                scopePolygonList.add(data.bid2BlockMap.get(blockId).polygon);
            }

            for (int idx = initialPolygonNum; idx < blockIdList.size(); idx+=1) {
                long blockId = blockIdList.get(idx);

                // 如果绿地或者水体面积占比过大则过滤
                if (data.bid2BlockMap.get(blockId).greenPercent > 0.6 || data.bid2BlockMap.get(blockId).waterPercent > 0.6) {
                    continue;
                }

                scopeBlockList.add(blockId);
                scopePolygonList.add(data.bid2BlockMap.get(blockId).polygon);

                Polygon currConcavePolygon = GeoUtils.getPolygonListConcaveHull(scopePolygonList);
                double currPolygonArea = GeoUtils.getArea(currConcavePolygon);
                if (currPolygonArea > MAX_AREA_THRESHOLD) {
                    break;
                }
            }

            scopeBlockList.remove(scopeBlockList.size()-1);
            scopePolygonList.remove(scopePolygonList.size()-1);

            poiScopeBlockList.put(poiId, scopeBlockList);
            poiBScopeMap.put(poiId, GeoUtils.getPolygonListConcaveHull(scopePolygonList));
        }
    }

    /**
     * 将AOI List转为AMap格式字符串
     * @param aoiIdList AOI List
     * @return AMap格式字符串
     */
    private String getAoiListAMap(List<Long> aoiIdList) {
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

    /**
     * 将Block List转为AMap格式字符串
     * @param blockIdList Block List
     * @return AMap格式字符串
     */
    private String getBlockListAMap(List<Long> blockIdList) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int idx = 0; idx < blockIdList.size()-1; idx += 1) {
            long blockId = blockIdList.get(idx);
            sb.append(GeoUtils.getPolygonAMAP(data.bid2BlockMap.get(blockId).polygon));
            sb.append(",");
        }
        long lastBlockId = blockIdList.get(blockIdList.size()-1);
        sb.append(GeoUtils.getPolygonAMAP(data.bid2BlockMap.get(lastBlockId).polygon));
        sb.append("]");
        return sb.toString();
    }

    /**
     * 将AOI List信息转换为JSON字符串
     * @param poiId 商家ID
     * @param aoiList AOI List
     * @return AOI List 对应的JSON字符串
     */
    private String getAoiInfoString(long poiId, List<Long> aoiList) {
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
     * 将Block List信息转换为JSON字符串
     * @param poiId 商家ID
     * @param blockList Block List
     * @return Block List 对应的JSON字符串
     */
    private String getBlockInfoString(long poiId, List<Long> blockList) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int idx = 0; idx < blockList.size()-1; idx+=1) {
            long blockId = blockList.get(idx);
            double dist = getPoiBlockDist(poiId, blockId);
            String infoStr = data.bid2BlockMap.get(blockId).toJSONString(dist);
            sb.append(infoStr).append(",");
        }
        long lastBlockId = blockList.get(blockList.size()-1);
        double dist = getPoiBlockDist(poiId, lastBlockId);
        sb.append(data.bid2BlockMap.get(lastBlockId).toJSONString(dist)).append("]");
        return sb.toString();
    }

    /**
     * 获取一定距离范围内不被选中的AOI集合，转化为AMap格式字符串
     * @param poiId 商家ID
     * @param maxDist 最大距离约束
     * @return AMap格式字符串
     */
    private String getNearbyNotInScopeAoiAMap(long poiId, double maxDist) {
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

    /**
     * 获取一定距离范围内不被选中的Block集合，转化为AMap格式字符串
     * @param poiId 商家ID
     * @param maxDist 最大距离约束
     * @return AMap格式字符串
     */
    private String getNearbyNotInScopeBlockAMap(long poiId, double maxDist) {
        List<Long> blockIdList = poiScopeBlockList.get(poiId);
        Set<Long> blockIdSet = Sets.newHashSet(blockIdList);
        List<Long> allBlockList = data.pid2BlockListMap.get(poiId);

        List<Polygon> notInScopeBlockPolygonList = Lists.newArrayList();
        for (long blockId : allBlockList) {
            if (blockIdSet.contains(blockId)){
                continue;
            }
            Polygon blockPolygon = data.bid2BlockMap.get(blockId).polygon;
            if (getPoiBlockDist(poiId, blockId) <= maxDist) {
                notInScopeBlockPolygonList.add(blockPolygon);
            }
//            notInScopeBlockPolygonList.add(blockPolygon);
        }
        return GeoUtils.getPolygonListAMap(notInScopeBlockPolygonList);
    }

    private String getNearbyNotInScopeAoiInfo(long poiId, double maxDist) {
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
        return getAoiInfoString(poiId, aoiList);
    }

    private String getNearbyNotInScopeBlockInfo(long poiId, double maxDist) {
        List<Long> blockIdList = poiScopeBlockList.get(poiId);
        Set<Long> blockIdSet = Sets.newHashSet(blockIdList);
        List<Long> allBlockList = data.pid2BlockListMap.get(poiId);

        List<Long> blockList = Lists.newArrayList();
        for (long blockId : allBlockList) {
            if (blockIdSet.contains(blockId)){
                continue;
            }
            if (getPoiBlockDist(poiId, blockId) <= maxDist) {
                blockList.add(blockId);
            }
//            blockList.add(blockId);
        }
        return getBlockInfoString(poiId, blockList);
    }

    public void write(long poiId, String oldScope, String cityName, double maxDistThres) {
        // 用来存放商家坐标点的AMap文件的URL（js文件）
        String poiPointPath = "display/revenue/js/" + cityName + "/poi_point.js";

        // 用来存放商家当前配送范围的AMap文件的URL（js文件）
        String oldScopePath = "display/revenue/js/" + cityName + "/original_scope.js";

        // 用来存放由AOI新生成的商家配送范围的AMap文件的URL（js文件）
        String newAoiScopePath = "display/revenue/js/" + cityName + "/poi_aoi_scope.js";
        // 用来存放由Block新生成的商家配送范围的AMap文件的URL（js文件）
        String newBlockScopePath = "display/revenue/js/" + cityName + "/poi_block_scope.js";

        // 用来存放candidate AOIs但最终没有被选中的AOI信息 JSON 格式（js文件）
        String notInScopeAoiInfoPath = "display/revenue/js/" + cityName + "/not_in_scope_aoi_info.js";
        // 用来存放candidate AOIs最终被选中的AOI信息 JSON格式（js文件）
        String scopeAoiInfoPath = "display/revenue/js/" + cityName + "/scope_aoi_info.js";
        // 用来存放candidate AOIs但最终没有被选中的AOIs AMap格式（js文件）
        String notInScopeAoiPolygonPath = "display/revenue/js/" + cityName + "/not_in_scope_aoi.js";
        // 用来存放candidate AOIs最终被选中的AOIs AMap格式（js文件）
        String scopeAoiPolygonPath = "display/revenue/js/" + cityName + "/scope_aoi.js";

        // 用来存放candidate Blocks但最终没有被选中的Block信息 JSON 格式（js文件）
        String notInScopeBlockInfoPath = "display/revenue/js/" + cityName + "/not_in_scope_block_info.js";
        // 用来存放candidate Blocks最终被选中的Block信息 JSON格式（js文件）
        String scopeBlockInfoPath = "display/revenue/js/" + cityName + "/scope_block_info.js";
        // 用来存放candidate Blocks但最终没有被选中的Blocks AMap格式（js文件）
        String notInScopeBlockPolygonPath = "display/revenue/js/" + cityName + "/not_in_scope_block.js";
        // 用来存放candidate Blocks最终被选中的Blocks AMap格式（js文件）
        String scopeBlockPolygonPath = "display/revenue/js/" + cityName + "/scope_block.js";

        try {
            File f = new File("display/revenue/js/" + cityName);
            if (!f.exists()) {
                boolean mkdirs = f.mkdirs();
                if (!mkdirs) return;
            }

            // 旧范围的AMap文件
            String oldScopeAMap = GeoUtils.getPolygonAMAP(PolygonHelper.createPolygonFromArea(oldScope));
            BufferedWriter bw = new BufferedWriter(new FileWriter(oldScopePath));
            bw.write("var original_scope = " + oldScopeAMap);
            bw.close();

            // 新AOI范围的AMap文件
            String newScopeAoiAMap = GeoUtils.getPolygonAMAP(poiAScopeMap.get(poiId));
            bw = new BufferedWriter(new FileWriter(newAoiScopePath));
            bw.write("var poi_scope_aoi = " + newScopeAoiAMap + ";");
            bw.close();
            // 新Block范围的AMap文件
            String newScopeBlockAMap = GeoUtils.getPolygonAMAP(poiBScopeMap.get(poiId));
            bw = new BufferedWriter(new FileWriter(newBlockScopePath));
            bw.write("var poi_scope_block = " + newScopeBlockAMap + ";");
            bw.close();

            // AOI List 写入文件中
            String scopeAoiInfo = getAoiInfoString(poiId, poiScopeAoiList.get(poiId));
            bw = new BufferedWriter(new FileWriter(scopeAoiInfoPath));
            bw.write("var scope_aois_info = " + scopeAoiInfo + ";");
            bw.close();

            String notInScopeAoiInfo = getNearbyNotInScopeAoiInfo(poiId, maxDistThres);
            bw = new BufferedWriter(new FileWriter(notInScopeAoiInfoPath));
            bw.write("var not_in_scope_aois_info = " + notInScopeAoiInfo + ";");
            bw.close();

            String scopeAoiAMap = getAoiListAMap(poiScopeAoiList.get(poiId));
            bw = new BufferedWriter(new FileWriter(scopeAoiPolygonPath));
            bw.write("var scope_aois_amap = " + scopeAoiAMap + ";");
            bw.close();

            String notInScopeAoiAMap = getNearbyNotInScopeAoiAMap(poiId, maxDistThres);
            bw = new BufferedWriter(new FileWriter(notInScopeAoiPolygonPath));
            bw.write("var not_in_scope_aois_amap = " + notInScopeAoiAMap + ";");
            bw.close();

            // Block List 写入文件中
            String scopeBlockInfo = getBlockInfoString(poiId, poiScopeBlockList.get(poiId));
            bw = new BufferedWriter(new FileWriter(scopeBlockInfoPath));
            bw.write("var scope_blocks_info = " + scopeBlockInfo + ";");
            bw.close();

            String notInScopeBlockInfo = getNearbyNotInScopeBlockInfo(poiId, maxDistThres);
            bw = new BufferedWriter(new FileWriter(notInScopeBlockInfoPath));
            bw.write("var not_in_scope_blocks_info = " + notInScopeBlockInfo + ";");
            bw.close();

            String scopeBlockAMap = getBlockListAMap(poiScopeBlockList.get(poiId));
            bw = new BufferedWriter(new FileWriter(scopeBlockPolygonPath));
            bw.write("var scope_blocks_amap = " + scopeBlockAMap + ";");
            bw.close();

            String notInScopeBlockAMap = getNearbyNotInScopeBlockAMap(poiId, maxDistThres);
            bw = new BufferedWriter(new FileWriter(notInScopeBlockPolygonPath));
            bw.write("var not_in_scope_blocks_amap = " + notInScopeBlockAMap + ";");
            bw.close();

            // 商家坐标点
            String poiPointAMap = "[" + data.pid2PoiMap.get(poiId).getLng() + "," + data.pid2PoiMap.get(poiId).getLat() + "]";
            bw = new BufferedWriter(new FileWriter(poiPointPath));
            bw.write("var poi_point = " + poiPointAMap + ";");
            bw.close();

        } catch(Exception e) {
            e.printStackTrace();
        }
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

            String newScopeAMap = GeoUtils.getPolygonAMAP(poiAScopeMap.get(poiId));
            bw = new BufferedWriter(new FileWriter(newScopePath));
            bw.write("var poi_scope = " + newScopeAMap + ";");
            bw.close();

            String notInScopeAoiAMap = getNearbyNotInScopeAoiAMap(poiId, maxAoiDistThres);
            bw = new BufferedWriter(new FileWriter(notInScopeAoiPath));
            bw.write("var not_in_scope_aois = " + notInScopeAoiAMap + ";");
            bw.close();

            String scopeAoiAMap = getAoiListAMap(poiScopeAoiList.get(poiId));
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

    public void writeJsDataFileBlock(long poiId, String oldScope, String cityName, double maxAoiDistThres) {
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

            String newScopeAMap = GeoUtils.getPolygonAMAP(poiBScopeMap.get(poiId));
            bw = new BufferedWriter(new FileWriter(newScopePath));
            bw.write("var poi_scope = " + newScopeAMap + ";");
            bw.close();

            String notInScopeAoiAMap = getNearbyNotInScopeAoiAMap(poiId, maxAoiDistThres);
            bw = new BufferedWriter(new FileWriter(notInScopeAoiPath));
            bw.write("var not_in_scope_aois = " + notInScopeAoiAMap + ";");
            bw.close();

            String scopeAoiAMap = getAoiListAMap(poiScopeBlockList.get(poiId));
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

            String newScopeAMap = GeoUtils.getPolygonAMAP(poiAScopeMap.get(poiId));
            bw = new BufferedWriter(new FileWriter(newScopePath));
            bw.write("var poi_scope = " + newScopeAMap + ";");
            bw.close();

            String notInScopeAoiInfo = getNearbyNotInScopeAoiInfo(poiId, maxAoiDistThres);
            bw = new BufferedWriter(new FileWriter(notInScopeAoiInfoPath));
            bw.write("var not_in_scope_aoi_info = " + notInScopeAoiInfo + ";");
            bw.close();

            String scopeAoiInfo = getAoiInfoString(poiId, poiScopeAoiList.get(poiId));
            bw = new BufferedWriter(new FileWriter(scopeAoiInfoPath));
            bw.write("var scope_aoi_info = " + scopeAoiInfo + ";");
            bw.close();

            String notInScopeAoiAMap = getNearbyNotInScopeAoiAMap(poiId, maxAoiDistThres);
            bw = new BufferedWriter(new FileWriter(notInScopeAoiPolygonPath));
            bw.write("var not_in_scope_aois = " + notInScopeAoiAMap + ";");
            bw.close();

            String scopeAoiAMap = getAoiListAMap(poiScopeAoiList.get(poiId));
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
    @Deprecated
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
        if ((aoiDistMap != null) && (!aoiDistMap.isEmpty())) {
            String key = String.valueOf(poiId) + "_" + String.valueOf(aoiId);
            if (aoiDistMap.containsKey(key)) {
                dist = aoiDistMap.get(key);
            }
        }
        return dist;
    }

    private double getPoiBlockDist(long poiId, long blockId) {
        if (!data.pid2PoiMap.containsKey(poiId)
                || (!data.bid2BlockMap.containsKey(blockId))) {
            return -1.0;
        }

        double dist = GeoUtils.getPointDist(data.pid2PoiMap.get(poiId).getPoiPoint(), data.bid2BlockMap.get(blockId).centroidPoint);
        if (blockDistMap != null && (!blockDistMap.isEmpty())) {
            String key = String.valueOf(poiId) + "_" + String.valueOf(blockId);
            if (blockDistMap.containsKey(key)) {
                dist = blockDistMap.get(key);
            }
        }
        return dist;
    }
}
