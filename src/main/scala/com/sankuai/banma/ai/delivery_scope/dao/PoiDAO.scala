package com.sankuai.banma.ai.delivery_scope.dao
import org.apache.spark.sql.{DataFrame, SparkSession}

/**
  * Created by dingxuetao on 2019/8/6.
  */
class PoiDAO extends IFeatureFetcher {

  /**
    * 获取指定开始日期到结束日期的商家数据
    *
    * @param ss        spark会话对象
    * @param startDate 数据开始日期 (demo: 20190805)
    * @param endDate   数据结束日期 (demo: 20190806)
    * @return 指定的数据，具体格式在具体对象中定义
    * (
    *   poi_id                  商家ID
    *   city_id                 城市ID
    *   poi_name                商家名称
    *   longitude               商家经度
    *   latitude                商家纬度
    *   bm_delivery_area_id     商家所属区域ID
    *   delivery_type           商家所属区域对应的配送类型(配送类型 1001:加盟 1002:自营 1003:众包 1004:城市代理 2002:快送)
    *   datype_id               配送区域业务类型 1为校园,2为白领,0为未知
    * )
    */
  override def getData(ss: SparkSession, startDate: String, endDate: String): DataFrame = {
    var poiDF = ss.sql(s"""SELECT poi_basic_info.poi_id as poi_id, city_id, poi_name, longitude, latitude, bm_delivery_area_id, delivery_type, datype_id
                         | FROM
                         | (SELECT distinct poi_id, city_id, poi_name, longitude, latitude
                         | FROM mart_peisong.attr_poi_info_day
                         | where dt>=$endDate and dt<=$endDate) poi_basic_info
                         | JOIN
                         | (select distinct wm_poi_id, bm_delivery_area_id, delivery_type, datype_id
                         | from mart_peisong.dim_delivery_area_poi_snapshot
                         | where dt>=$endDate and dt<=$endDate) poi_da_relation
                         | ON poi_basic_info.poi_id=poi_da_relation.wm_poi_id""".stripMargin
    )
    return poiDF
  }
}
