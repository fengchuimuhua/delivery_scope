package com.sankuai.banma.ai.utils;

import com.vividsolutions.jts.geom.Point;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by zhangrunfeng on 2019-08-26
 */
public class AmapUtils {

    private static String[] keyArrays = {
            "ad2862bae80bd752c0014b5a1040092e",
            "5116c30e9ae1bcbe79783a913d992fa2",
            "937bb723d1abfa9881d57a4ec6155e2f",
            "eaf742ba620566c1dbabaeb31ac40dd3",
            "f530fb58d6dc282eb09f329e3a220ff9",
            "dd6082bac070516038b3139317d59de2",
            "f3aabfd1f4bcb6ccc42b185df25e3401",
            "ec836433d65bec4157f21ecfb7f72ba9",
            "b9c0b87f902bd5feed1093972a4fb135",
            "bdd1327c474d8623cc913e60a98a1a5d",
            "2c2c1ccccad845db0eb6ce971a1bf711",
            "7b236e7de1ed28dc246ad87d16b7e1ab",
            "f60988f71f7e897f68a73d4accfe2eb3",
            "08bb5d003c6c60ed79ddd57514ea81ea",
            "df3d3b3cadb22c413534deefbe2095a6",
            "2878c2dc14c4dcb3cad53df03d25a0d9",
            "44b0738cafc0a53e9ab7898e44c4a39d",
            "0ec83e005450e84befec319861cc31ae",
            "78687609ec2c4ff3de3821af3b8e7767",
            "44b97d31443d836356e2e54294e034f8"
    };

    /**
     * 返回输入地址address的经纬度信息, 格式是 经度,纬度
     * @param address 待查询地址
     * @return 经纬度 "lng,lat"
     */
    private static String getLngLat(String key, String address){
        String queryUrl = "http://restapi.amap.com/v3/geocode/geo?key=" + key + "&address="+address;
        String queryResult = getResponse(queryUrl);  //高德接品返回的是JSON格式的字符串

        JSONObject jo = new JSONObject().fromObject(queryResult);
        JSONArray ja = jo.getJSONArray("geocodes");
        return new JSONObject().fromObject(ja.getString(0)).get("location").toString();
    }

    public static double getDistance(int index, Point startPoint, Point endPoint) {
        int idx = index / 1900;
        String key = keyArrays[idx];
        try {
            return getDistance(key, startPoint, endPoint);
        } catch(Exception e) {
            return -1.0;
        }
    }

    /**
     * 调用HTTP接口请求两点之间的骑行距离，单位米(m)
     * @param startLngLat 起始点经纬度 "lng,lat"
     * @param endLngLat 结束点经纬度 "lng,lat"
     * @return 两点之间的经纬度
     */
    public static long getDistance(String key, String startLngLat, String endLngLat) throws Exception{
        String queryUrl = "https://restapi.amap.com/v4/direction/bicycling?origin=" + startLngLat + "&destination=" + endLngLat + "&output=json&key=" + key;
        String queryResult = getResponse(queryUrl);
        JSONObject jo = new JSONObject().fromObject(queryResult);
        JSONArray ja =  (JSONArray) jo.getJSONObject("data").get("paths");
        String distStr = (new JSONObject().fromObject(ja.getString(0))).get("distance").toString();

        return Long.parseLong(distStr);
    }

    /**
     * 调用高德API计算两点之间的骑行距离
     * @param startPoint 起始点坐标 JTS Point
     * @param endPoint 结束点坐标 JTS Point
     * @return 两点间的骑行距离
     */
    public static long getDistance(String key, Point startPoint, Point endPoint) throws Exception {
        String startPointStr = String.valueOf(startPoint.getY() + "," + startPoint.getX());
        String endPointStr = String.valueOf(endPoint.getY() + "," + endPoint.getX());

        return getDistance(key, startPointStr, endPointStr);
    }

    private static String getResponse(String serverUrl){
        //用Java发起HTTP请求，并返回json格式的结果
        StringBuilder result = new StringBuilder();
        try {
            URL url = new URL(serverUrl);
            URLConnection conn = url.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));

            String line;
            while((line = in.readLine()) != null){
                result.append(line);
            }
            in.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return result.toString();
    }

}
