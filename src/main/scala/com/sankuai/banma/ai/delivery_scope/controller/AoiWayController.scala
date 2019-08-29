package com.sankuai.banma.ai.delivery_scope.controller

import com.sankuai.banma.ai.delivery_scope.constant.Config
import com.sankuai.banma.ai.delivery_scope.dao.{AoiDAO, IFeatureFetcher, PoiDAO}
import org.apache.spark.SparkConf
import org.apache.spark.sql.{DataFrame, Row, SparkSession}

/**
  * 通过AOI画范围的控制器
  * Created by dingxuetao on 2019/8/13.
  */
class AoiWayController {

  val conf = new Config

  def producePoiArea(startDate: String, endDate: String, areaLimit: Float): Unit = {
    /* step 0. 初始化SPARK环境*/
    val ss = SparkSession.builder().config(new SparkConf().setAppName(conf.SPARK_APP_NAME)).enableHiveSupport().getOrCreate()

    /* step 1. 获取基础数据 */
    val aoiDAO : IFeatureFetcher = new AoiDAO
    val poiDAO : IFeatureFetcher = new PoiDAO
    var aoiDF : DataFrame = aoiDAO.getData(ss, startDate, endDate)
    var poiDF : DataFrame = poiDAO.getData(ss, startDate, endDate)
    println("AOI number : " +  aoiDF.count())
    println("POI number : " +  poiDF.count())

    aoiDF = aoiDF.filter("city_id=610100")
    poiDF = poiDF.filter("city_id=610100")
    println("AOI number : " +  aoiDF.count())
    println("POI number : " +  poiDF.count())

    /* step 2. 合并AOI和POI数据(分城市合并) */
    var poiPlusAoiDF : DataFrame = aoiDF.join(poiDF, aoiDF("city_id") === poiDF("city_id"), "inner").select(
      aoiDF("bm_aoi_id").as("aoi_id"),
      aoiDF("city_id").as("city_id"),
      aoiDF("polygon").as("polygon"),
      aoiDF("center_point").as("aoi_center_point"),
      aoiDF("address").as("aoi_addr"),
      aoiDF("name").as("aoi_name"),
      aoiDF("ordnum").as("aoi_demand"),
      poiDF("poi_id").as("poi_id"),
      poiDF("poi_name").as("poi_name"),
      poiDF("longitude").as("lng"),
      poiDF("latitude").as("lat"),
      poiDF("bm_delivery_area_id").as("da_id"),
      poiDF("delivery_type").as("delivery_type")
    )
    println("AOI * POI number : " + poiPlusAoiDF.count())
    poiPlusAoiDF = poiPlusAoiDF.filter("delivery_type=1001 OR delivery_type=1002 OR delivery_type=1003 OR delivery_type=2002")
    println("AOI * POI number (After filtering delivery_type) : " + poiPlusAoiDF.count())
    poiPlusAoiDF.show(20, false)

    /* step 3. POI * AOI 打分 */
//    poiPlusAoiDF.rdd.map {
//      case Row(aoi_id: Int) => Row()
//    }
//    poiPlusAoiDF = poiPlusAoiDF.map { poiPlusAoi => Row(
//      poiPlusAoi.getAs[Int]("aoi_id"),
//      poiPlusAoi.getAs[Int]("city_id"),
//      poiPlusAoi.getAs[String]("polygon"),
//      poiPlusAoi.getAs[String]("aoi_center_point"),
//      poiPlusAoi.getAs[String]("aoi_addr"),
//      poiPlusAoi.getAs[String]("aoi_name"),
//      poiPlusAoi.getAs[Int]("aoi_demand"),
//      poiPlusAoi.getAs[Int]("poi_id"),
//      poiPlusAoi.getAs[String]("poi_name"),
//      poiPlusAoi.getAs[Int]("lng"),
//      poiPlusAoi.getAs[Int]("lat"),
//      poiPlusAoi.getAs[Int]("da_id"),
//      poiPlusAoi.getAs[Int]("delivery_type"),
//      300
//    ) }
//    poiPlusAoiDF.show(20, false)
//    poiPlusAoiDF.map(
//      case (aoi_id: Int, city_id: Int, polygon: String, aoi_center_point: String, aoi_addr: String, aoi_name: String, aoi_demand: Int) =>
//    )

  }


}
