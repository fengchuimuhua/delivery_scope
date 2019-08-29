package com.sankuai.banma.ai.delivery_scope.main

import com.sankuai.banma.ai.delivery_scope.controller.AoiWayController
import com.sankuai.banma.ai.delivery_scope.dao.AoiDAO
import org.apache.spark.SparkConf
import org.apache.spark.sql.{DataFrame, SparkSession}

/**
  * 程序主入口
  * Created by dingxuetao on 2019/8/11.
  */
object Main {

  def main(args: Array[String]): Unit = {
    if (args.length != 3) {
      println("delivery area [start_date] [end_date] [limit_area]")
    }
    val startDate = args(0)
    val endDate = args(1)
    val limit_area = args(2)
    val aoiWayController : AoiWayController = new AoiWayController
    aoiWayController.producePoiArea(startDate, endDate, limit_area.toFloat)
  }

}
