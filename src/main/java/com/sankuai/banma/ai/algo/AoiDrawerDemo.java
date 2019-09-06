package com.sankuai.banma.ai.algo;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.meituan.sankuai.scope.algorithm.utils.GeographicUtils;
import com.sankuai.meituan.banma.shiparea.helper.PolygonHelper;
import org.apache.commons.collections.CollectionUtils;
import com.vividsolutions.jts.algorithm.ConvexHull;
import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequenceFactory;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import org.apache.commons.io.output.FileWriterWithEncoding;
import org.opensphere.geometry.algorithm.ConcaveHull;

import java.io.*;
import java.util.*;

/**
 * Created by dingxuetao on 2019/8/15.
 */
public class AoiDrawerDemo {

    public AoiDrawerDemo() {
        pid2PoiMap = Maps.newHashMap();
        aid2AoiMap = Maps.newHashMap();
        pid2aidlistMap = Maps.newHashMap();
        geometryFactory = new GeometryFactory();
        wktReader = new WKTReader( geometryFactory );
    }

    private Map<Integer, Poi> pid2PoiMap;
    private Map<Integer, Aoi> aid2AoiMap;
    private Map<Integer, List<Aoi>> pid2aidlistMap;
    private GeometryFactory geometryFactory;
    private WKTReader wktReader;
    private static final CoordinateArraySequenceFactory coordinateArraySequenceFactory = CoordinateArraySequenceFactory.instance();
    private static final double AREA_CONSTRAIT = 35.0 * Math.pow(10, 6);


    public void run() {

        Map<Integer, List<Aoi>> pid2finalaoiMap = Maps.newHashMap();
        /* step 1. 准备数据 */
        try {
            prepareData();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }

       for (Map.Entry<Integer, List<Aoi>> entry : pid2aidlistMap.entrySet()) {

            /* step 2. 打分 */
           int pid = entry.getKey();
           pid2finalaoiMap.put(pid, new ArrayList<>());
           List<Aoi> aoiList = entry.getValue();
           for (Aoi aoi : aoiList) {
               double dist = getDistance(pid2PoiMap.get(pid).lng, pid2PoiMap.get(pid).lat, aoi.centroid.getX(), aoi.centroid.getY()) / 1000.0;
               if (dist <= 3) {
                   aoi.score = aoi.ordnum / Math.pow(2, dist);
               } else {
                   aoi.score = aoi.ordnum / Math.pow(3, dist);
               }
               // aoi.score = 0 - getDistance(pid2PoiMap.get(pid).lng, pid2PoiMap.get(pid).lat, aoi.centroid.getX(), aoi.centroid.getY()) / 1000.0;
           }
           Collections.sort(aoiList, new Comparator<Aoi>() {
               @Override
               public int compare(Aoi aoi1, Aoi aoi2) {
                   if (aoi1.score == aoi2.score) {
                       return 0;
                   } else if (aoi1.score > aoi2.score) {
                       return -1;
                   } else {
                       return 1;
                   }
               }
           });
           /* step 3. 选择AOI */
           double areaSum = 0;
           int idx = 0;
           while (areaSum <= AoiDrawerDemo.AREA_CONSTRAIT) {
               pid2finalaoiMap.get(pid).add(aoiList.get(idx));
               areaSum += aoiList.get(idx).area;
               idx += 1;
           }
           System.out.println("Select aoi num : " + pid2finalaoiMap.get(pid).size());
           List<Aoi> finalAoiList = pid2finalaoiMap.get(pid);
           /* step 4. 合并AOI */
           List<Polygon> aoiPolygonList = Lists.newArrayList();
           for (Aoi aoi : finalAoiList) {
               aoiPolygonList.add(aoi.polygon);
           }
           int chooseK = 41;
           while (merge(chooseK, finalAoiList, false) < AoiDrawerDemo.AREA_CONSTRAIT && chooseK <= finalAoiList.size()) {
               chooseK++;
           }
           merge(chooseK - 1, finalAoiList, true);

           //System.out.println(geometryCollection.union());
       }




//        try {
//            Polygon polygon = (Polygon)wktReader.read("POLYGON ((32.243293 118.321203, 32.243186 118.321308, 32.243183 118.324822, 32.238413 118.325, 32.238303 118.33061, 32.238424 118.331471, 32.238433 118.3359, 32.241239 118.336007, 32.241346 118.336253, 32.243183 118.336257, 32.243192 118.341203, 32.242316 118.341203, 32.242223 118.341328, 32.242351 118.341519, 32.243322 118.341517, 32.243429 118.34662, 32.243662 118.349197, 32.244562 118.353234, 32.244687 118.35329, 32.247211 118.352228, 32.247465 118.352288, 32.249684 118.351456, 32.250135 118.351271, 32.25014 118.351285, 32.250295 118.351227, 32.254506 118.349635, 32.256538 118.354308, 32.258573 118.357346, 32.259954 118.355706, 32.261696 118.354323, 32.26433 118.352083, 32.269502 118.347053, 32.271174 118.35064, 32.271382 118.350482, 32.271237 118.35022, 32.271727 118.35022, 32.273248 118.349066, 32.280579 118.343499, 32.280579 118.342853, 32.279589 118.341065, 32.278568 118.339222, 32.27693 118.339193, 32.277175 118.338736, 32.277555 118.337748, 32.277715 118.337704, 32.277718 118.337686, 32.277603 118.337476, 32.2751 118.326279, 32.272629 118.315841, 32.271254 118.30931, 32.270965 118.30902, 32.268218 118.307647, 32.266845 118.307647, 32.266736 118.308273, 32.266796 118.307388, 32.266655 118.307094, 32.266464 118.306889, 32.261575 118.304999, 32.261133 118.305259, 32.261013 118.30775, 32.2611 118.310643, 32.256492 118.308965, 32.253753 118.308344, 32.250781 118.308026, 32.249336 118.308066, 32.249173 118.308239, 32.249043 118.31654, 32.248416 118.31662, 32.248509 118.316797, 32.249043 118.316806, 32.249032 118.32084, 32.243741 118.320743, 32.243293 118.321203))");
//            System.out.println(calculateAcreage(polygon));
//            System.out.println(polygon.getCentroid());
//            System.out.println(polygon.getArea());
//        } catch (ParseException e) {
//            e.printStackTrace();
//        }
    }

    private double merge(int len, List<Aoi> finalAoiList, boolean isPrint) {
        List<Polygon> aoiPolygonList = Lists.newArrayList();
        for (int i = 0; i < len; i++) {
            aoiPolygonList.add(finalAoiList.get(i).polygon);
        }
        printPolygonListWithAMapFormat(aoiPolygonList, isPrint);
        GeometryCollection geometryCollection = (GeometryCollection) geometryFactory.buildGeometry(aoiPolygonList);
        ConvexHull convexHull = new ConvexHull(geometryCollection.union());
        Polygon convexPolygon = (Polygon) convexHull.getConvexHull();
        printPolygonWithAMapFormat(convexPolygon, isPrint);
        /* Concave Hull*/
        ConcaveHull concaveHull = new ConcaveHull(geometryCollection, 0.005);
        Polygon concavePolygon = (Polygon) concaveHull.getConcaveHull();
        printPolygonWithAMapFormat(concavePolygon, isPrint);
           /* step 5. 测试面积约束 */
        double mergeArea = calculateAcreage(convexPolygon);
        if (isPrint) {
            System.out.println("AOI Length : " + len + ", area: " + mergeArea);
        }
        return mergeArea;
    }

    private class Poi {
        public int poi_id;
        public String poi_name;
        public double lng;
        public double lat;
        public Point location;
    }

    private class Aoi {
        public int aoi_id;
        public String polygon_str;
        public String center_point;
        public String addr;
        public String name;
        public int ordnum;
        public Polygon polygon;
        public Point centroid;
        public double score; // 用于aoi和poi的打分, 临时存储
        public double area; // 面积
    }

    private String processWKTInput(String str) throws ParseException {
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

    private String printPolygonWithAMapFormat(Polygon polygon, boolean isPrint) {
        StringBuffer polygonBF = new StringBuffer("[");
        for (int i = 0; i < polygon.getCoordinates().length; i++) {
            Coordinate c = polygon.getCoordinates()[i];
            polygonBF.append("[" + c.y + ", " + c.x + "]");
            if (i < polygon.getCoordinates().length - 1) {
                polygonBF.append(",");
            }
        }
        polygonBF.append("]");
        if (isPrint) {
            System.out.println(polygonBF.toString());
        }
        return polygonBF.toString();
    }

    private void printPolygonListWithAMapFormat(List<Polygon> polygonList, boolean isPrint) {
        StringBuffer polygonListBF = new StringBuffer("[");
        for (int k = 0; k < polygonList.size(); k++) {
            Polygon polygon = polygonList.get(k);
            polygonListBF.append(printPolygonWithAMapFormat(polygon, false));
            if (k < polygonList.size() - 1) {
                polygonListBF.append(",");
            }
        }
        polygonListBF.append("]");
        if (isPrint) {
            System.out.println(polygonListBF.toString());
        }
    }

    private void writePolygonListIntoFile(List<Polygon> polygonList, String path) {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(path));
            bw.write("Var polygonList = [");
            for (int idx = 0; idx < polygonList.size()-1; idx+=1) {
                bw.write(printPolygonWithAMapFormat(polygonList.get(idx), false));
                bw.write(",");
            }
            bw.write(printPolygonWithAMapFormat(polygonList.get(polygonList.size()-1), false));
            bw.write("]");
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void prepareData() throws IOException, ParseException {
        BufferedReader br = new BufferedReader(new FileReader("beijing_poi_aoiinfo.txt"));
        String line = null;
        Map<Integer, Set<Integer>> pid2aidsetMap = Maps.newHashMap();
        while ((line = br.readLine()) != null) {
            String[] info = line.split("\t");
            int aoi_id = Integer.parseInt(info[0]);
            String polygon_str = info[1];
            String center_point = info[2];
            String aoi_addr = info[3];
            String aoi_name = info[4];
            int aoi_demand = Integer.parseInt(info[5]);
            int poi_id = Integer.parseInt(info[6]);

            // TODO for test
            if (poi_id != 3696913) {
                continue;
            }

            String poi_name = info[7];
            double poi_lng = Double.parseDouble(info[8]) / Math.pow(10, 6);
            double poi_lat = Double.parseDouble(info[9]) / Math.pow(10, 6);
            // 商家
            if (! pid2PoiMap.containsKey(poi_id)) {
                Poi poi = new Poi();
                poi.poi_id = poi_id;
                poi.poi_name = poi_name;
                poi.lng = poi_lng;
                poi.lat = poi_lat;
                poi.location = geometryFactory.createPoint(new Coordinate(poi.lng, poi.lat));
                pid2PoiMap.put(poi.poi_id, poi);
            }
            // AOI
            if ( ! aid2AoiMap.containsKey(aoi_id)) {
                Aoi aoi = new Aoi();
                aoi.aoi_id = aoi_id;
                aoi.polygon_str = polygon_str;
                aoi.center_point = center_point;
                aoi.addr = aoi_addr;
                aoi.name = aoi_name;
                aoi.ordnum = aoi_demand;

                String trueWKTStr = processWKTInput(aoi.polygon_str);
                aoi.polygon = (Polygon) wktReader.read(trueWKTStr);
                aoi.centroid = (Point) wktReader.read(center_point);
                aoi.area = calculateAcreage(aoi.polygon);
                aid2AoiMap.put(aoi_id, aoi);
            }
            // 商家 对应 AOI list
            if (! pid2aidlistMap.containsKey(poi_id)) {
                pid2aidlistMap.put(poi_id, new ArrayList<>());
                pid2aidsetMap.put(poi_id, new HashSet<>());
            }
            if (! pid2aidsetMap.get(poi_id).contains(aoi_id)) {
                pid2aidlistMap.get(poi_id).add(aid2AoiMap.get(aoi_id));
                pid2aidsetMap.get(poi_id).add(aoi_id);
            }
        }
        System.out.println("Poi num : " + pid2PoiMap.keySet().size());
        System.out.println("Aoi num : " + aid2AoiMap.keySet().size());
        for (Map.Entry<Integer, List<Aoi>> entry : pid2aidlistMap.entrySet()) {
            System.out.println("poi : " + entry.getKey() + "; Aoi num : " + entry.getValue().size());
        }

    }

    /**
     * 获取指定几何图形的实际面积
     * @param geometry: 几何图形
     * @return 几何图形的面积
     */
    public double getArea(Geometry geometry) {
        return PolygonHelper.getArea(geometry);
    }


    public double calculateAcreage(Geometry geometry) {
        if(geometry != null && (geometry instanceof Polygon || geometry instanceof MultiPolygon)) {
            ArrayList convertedCoordinates = new ArrayList(geometry.getNumPoints() + 1);
            double minX = 2.147483647E9D;
            double minY = 2.147483647E9D;
            Coordinate[] fc = geometry.getCoordinates();
            int lc = fc.length;

            int convertedPolygon;
            Coordinate acreage;
            for(convertedPolygon = 0; convertedPolygon < lc; ++convertedPolygon) {
                acreage = fc[convertedPolygon];
                if(minX > acreage.x) {
                    minX = acreage.x;
                }

                if(minY > acreage.y) {
                    minY = acreage.y;
                }
            }

            fc = geometry.getCoordinates();
            lc = fc.length;

            for(convertedPolygon = 0; convertedPolygon < lc; ++convertedPolygon) {
                acreage = fc[convertedPolygon];
                double distancey = getDistance(acreage.x, minY, acreage.x, acreage.y);
                double distancex = getDistance(minX, acreage.y, acreage.x, acreage.y);
                convertedCoordinates.add(new Coordinate(distancex, distancey));
            }

            if(convertedCoordinates.size() < 3) {
                return 0.0D;
            } else {
                Coordinate var15 = (Coordinate)convertedCoordinates.get(0);
                Coordinate var16 = (Coordinate)convertedCoordinates.get(convertedCoordinates.size() - 1);
                if(!var15.equals2D(var16)) {
                    convertedCoordinates.add(var15);
                }

                Polygon var17 = createFixedPolygon((Coordinate[])convertedCoordinates.toArray(new Coordinate[0]));
                double var18 = var17.getArea();
                return var18;
            }
        } else {
            return 0.0D;
        }
    }

    private double rad(double d) {
        return d * 3.141592653589793D / 180.0D;
    }

    private double pow2(double val) {
        return val * val;
    }

    public double getDistance(double lat1, double lng1, double lat2, double lng2) {
        double radLat1 = rad(lat1);
        double radLat2 = rad(lat2);
        double a = radLat1 - radLat2;
        double b = rad(lng1) - rad(lng2);
        double s = 2.0D * Math.asin(Math.sqrt(pow2(Math.sin(a / 2.0D)) + Math.cos(radLat1) * Math.cos(radLat2) * pow2(Math.sin(b / 2.0D))));
        s = (double)Math.round(s * 6.378137E10D) / 10000.0D;
        return s;
    }

    public Polygon createFixedPolygon(Coordinate... coordinates) {
        Polygon polygon = new Polygon(createLinearRing(createCoordinateSequence(coordinates)), (LinearRing[])null, geometryFactory);
        return polygon;
    }

    public LinearRing createLinearRing(CoordinateSequence coordinateSequence) {
        return coordinateSequence != null && coordinateSequence.size() >= 4?new LinearRing(coordinateSequence, geometryFactory):null;
    }

    public CoordinateSequence createCoordinateSequence(Coordinate... coordinates) {
        ArrayList coordinateList = Lists.newArrayList(coordinates);
        return createCoordinateSequence2(coordinateList);
    }

    public CoordinateSequence createCoordinateSequence2(List<Coordinate> coordinates) {
        if(!CollectionUtils.isEmpty(coordinates) && coordinates.size() >= 4) {
            if(((Coordinate)coordinates.get(0)).equals(coordinates.get(coordinates.size() - 1))) {
                return coordinateArraySequenceFactory.create((Coordinate[])coordinates.toArray(new Coordinate[0]));
            } else {
                ArrayList newList = Lists.newArrayList(coordinates);
                newList.add(coordinates.get(0));
                return coordinateArraySequenceFactory.create((Coordinate[])newList.toArray(new Coordinate[0]));
            }
        } else {
            return null;
        }
    }

    public static void main(String[] args) {
//        String scopeJSON = "[{\"x\":34178796,\"y\":108938769},{\"x\":34179183,\"y\":108935642},{\"x\":34183255,\"y\":108922905},{\"x\":34195500,\"y\":108915344},{\"x\":34220621,\"y\":108906578},{\"x\":34245148,\"y\":108904070},{\"x\":34290838,\"y\":108902858},{\"x\":34292124,\"y\":108902886},{\"x\":34292616,\"y\":108902914},{\"x\":34347203,\"y\":108925821},{\"x\":34347294,\"y\":108931971},{\"x\":34347302,\"y\":108933828},{\"x\":34347297,\"y\":108936571},{\"x\":34347272,\"y\":108937952},{\"x\":34335879,\"y\":108947085},{\"x\":34313784,\"y\":108960361},{\"x\":34313732,\"y\":108960383},{\"x\":34276378,\"y\":108971485},{\"x\":34259503,\"y\":108972435},{\"x\":34234514,\"y\":108971372},{\"x\":34202589,\"y\":108956449},{\"x\":34181653,\"y\":108945397},{\"x\":34180986,\"y\":108944349},{\"x\":34180877,\"y\":108944170},{\"x\":34178796,\"y\":108938769}]";
//        Geometry scopeGeo = PolygonHelper.createPolygonFromArea(scopeJSON);
//
//        AoiDrawerDemo a = new AoiDrawerDemo();
//        double area1 = a.getArea(scopeGeo);
//        double area2 = a.calculateAcreage(scopeGeo);
//
//        System.out.println(area1);
//        System.out.println(area2);


        new AoiDrawerDemo().run();
    }

}
