
/**
 * config
 */
var color = ['#AED363', '#ECDC5E', '#FD8C2F', '#C50A3C', '#501A48'];
var red_color = ['#ff6699', '#ff0066', '#cc0066', '#ff0000'];
var green_color = ['#99ff99', '#66ff66', '#33cc33', '#333300'];
var line_weight_conf = [3, 4, 5, 6];

function get_line_color(stat) {
	var c = color[0];
	var r = stat['revenue'] / stat['ordnum'];
	if (r > 0) {
		var idx = parseInt(r);
		if (idx > 3) {
			idx = 3;
		}
		c = red_color[idx];
	} else {
		var idx = parseInt(Math.abs(r));
		if (idx > 3) {
			idx = 3;
		}
		c = green_color[idx];
	}
	return c;
}

function get_line_weight(stat) {
	var r = stat['revenue'] / stat['ordnum'];
	var idx = parseInt(Math.abs(r));
	if (idx > 3) {
		idx = 3;
	}
	return line_weight_conf[idx];
}

/**
 * 根据餐馆的信息和指标类型获取餐馆应该显示的颜色
 * @param r 餐馆
 * @param metricType 指标类型
 */
function get_restaurant_color(r, metricType) {
    if (metricType == 'profit') {
        if (r['avg_profit'] <= -0.5) {
            return 'rgba(255,0,0,1)';
        } else if (r['avg_profit'] >= 0.5) {
            return 'rgba(0,255,0,1)';
        } else {
            return '#FFAA00';
        }
    } else if (metricType == 'deliveryTime') {
        if (r['delivery_total_interval'] >= 40) {
            return 'rgba(255,0,0,1)';
        } else if (r['delivery_total_interval'] <= 30) {
            return 'rgba(0,255,0,1)';
        } else {
            return '#FFAA00';
        }
    } else if (metricType == 'onTimeRatio') {
        if (r['relative_on_time_order_cnt'] / r['finish_order_cnt'] <= 0.9) {
            return 'rgba(255,0,0,1)';
        } else if (r['relative_on_time_order_cnt'] / r['finish_order_cnt'] >= 0.98) {
            return 'rgba(0,255,0,1)';
        } else {
            return '#FFAA00';
        }
    } else if (metricType == 'finishRatio') {
        if (r['finish_order_cnt'] / r['total_order_cnt'] <= 0.95) {
            return 'rgba(255,0,0,1)';
        } else if (r['finish_order_cnt'] / r['total_order_cnt'] >= 0.99) {
            return 'rgba(0,255,0,1)';
        } else {
            return '#FFAA00';
        }
    } else {
    	return 'rgba(255,255,0,1)';
	}
}

/**
 * 根据<aoi, 餐馆>信息和指定指标类型获取AOI应该显示的颜色
 * @param ar <aoi, 餐馆>
 * @param metricType 指标类型
 */
function get_aoi_color(ar, metricType) {
    if (metricType == 'profit') {
        if (ar['avg_profit'] <= -0.5) {
            return 'rgba(255,0,0,1)';
        } else if (ar['avg_profit'] >= 0.5) {
            return 'rgba(0,255,0,1)';
        } else {
            return '#FFAA00';
        }
    } else if (metricType == 'deliveryTime') {
        if (ar['delivery_total_interval'] >= 40) {
            return 'rgba(255,0,0,1)';
        } else if (ar['delivery_total_interval'] <= 30) {
            return 'rgba(0,255,0,1)';
        } else {
            return '#FFAA00';
        }
    } else if (metricType == 'onTimeRatio') {
        if (ar['relative_on_time_order_cnt'] / ar['finish_order_cnt'] <= 0.9) {
            return 'rgba(255,0,0,1)';
        } else if (ar['relative_on_time_order_cnt'] / ar['finish_order_cnt'] >= 0.98) {
            return 'rgba(0,255,0,1)';
        } else {
            return '#FFAA00';
        }
    } else if (metricType == 'finishRatio') {
        if (ar['finish_order_cnt'] / ar['total_order_cnt'] <= 0.95) {
            return 'rgba(255,0,0,1)';
        } else if (ar['finish_order_cnt'] / ar['total_order_cnt'] >= 0.99) {
            return 'rgba(0,255,0,1)';
        } else {
            return '#FFAA00';
        }
    } else {
        return 'rgba(255,255,0,1)';
    }
}