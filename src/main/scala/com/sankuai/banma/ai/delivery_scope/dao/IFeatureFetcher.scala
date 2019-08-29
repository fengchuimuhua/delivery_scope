package com.sankuai.banma.ai.delivery_scope.dao

import org.apache.spark.sql.{DataFrame, SparkSession}

/**
  * 特征获取器
  * Created by dingxuetao on 2019/8/6.
  */
trait IFeatureFetcher {

  /**
    * 获取指定开始日期到结束日期的数据
    * @param ss spark会话对象
    * @param startDate 数据开始日期 (demo: 20190805)
    * @param endDate 数据结束日期 (demo: 20190806)
    * @return 指定的数据，具体格式在具体对象中定义
    */
  def getData(ss: SparkSession, startDate: String, endDate: String): DataFrame

}
