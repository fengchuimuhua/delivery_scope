package com.sankuai.banma.ai.delivery_scope.dao
import org.apache.spark.sql.{DataFrame, SparkSession}

/**
  * 获取AOI相关数据
  * 包括: 外卖单量, 面积, 多边形描述, 包括其他属性(水体、山体等等)
  * Created by dingxuetao on 2019/8/6.
  */
class AoiDAO extends IFeatureFetcher {

  /**
    * 获取指定开始日期到结束日期的数据
    *
    * @param ss        spark会话对象
    * @param startDate 数据开始日期 (demo: 20190805)
    * @param endDate   数据结束日期 (demo: 20190806)
    * @return 指定的数据，具体格式在具体对象中定义
    * (
    *  bm_aoi_id        唯一标识,
    *  city_id          城市ID,
    *  polygon          aoi轮廓,
    *  center_point     aoi中心点,
    *  address          地址,
    *  name             名称,
    *  ordnum           [开始日期, 结束日期] 订单量
    * )
    */
  override def getData(ss: SparkSession, startDate: String, endDate: String): DataFrame = {
    var aoiAttrData = ss.sql(
       s"""  select bm_aoi_id, city_id, polygon, center_point, address, name, aoi_demand.ordnum as ordnum
          |  from
          |  (select bm_aoi_id, mt_aoi_id, city_id, polygon, center_point, address, name
          |  from
          |(select bm_aoi_id, mt_aoi_id, city_id, polygon, center_point, address, name, row_number() over (partition by bm_aoi_id order by dt desc) as rnum
          |from mart_peisongaoi.fact_bm_aoi_base_info_snap
          |WHERE dt>=$endDate and dt<=$endDate) tmp
          |where rnum=1) aoi_attr
          |JOIN
          |(select client_aoi_id, count(*) as ordnum
          |from
          |(SELECT platform_order_id, client_aoi_id
          |from
          |(SELECT platform_order_id, client_aoi_id, row_number() over (partition by platform_order_id order by client_aoi_id) as rnum
          |FROM
          |(SELECT bm_waybill_id, client_aoi_id
          |from mart_peisongpa.fact_waybill_nlp_and_space_data
          |where dt>=$startDate and dt<=$endDate) waybill_aoi
          |JOIN
          |(select platform_order_id, waybill_id
          |from mart_peisong.attr_waybill_day
          |where dt>=$startDate and dt<=$endDate and waybill_status=50) waybill
          |on waybill_aoi.bm_waybill_id=waybill.waybill_id) order_aoi_noclean
          |where rnum=1) order_aoi_clean
          |GROUP BY client_aoi_id) aoi_demand
          |on aoi_attr.bm_aoi_id=aoi_demand.client_aoi_id
       """.stripMargin
    )
    return aoiAttrData
  }
}
