package com.sankuai.banma.ai.delivery_scope.strategy

/**
  * Created by dingxuetao on 2019/8/14.
  */
class AoiPoiScore {

  /**
    * 打分策略
    * @param aoiDemand AOI的需求量
    * @param dist <poi, aoi> 距离，单位米
    * @return
    */
  def score(aoiDemand: Int, dist: Double): Double = {
    return aoiDemand / (Math.pow(1.5, dist / 1000))
  }

}
