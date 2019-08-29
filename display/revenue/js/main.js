var map = new AMap.Map('container',{
    resizeEnable: true,
    zoom: 14,
    center: [116.397428, 39.90923]
});

//创建信息展示窗体
var infoWindow = new AMap.InfoWindow({
     closeWhenClickMap: true
});

var infoWindow4AOI = new AMap.InfoWindow({
    closeWhenClickMap: true
});

AMap.plugin(['AMap.ToolBar','AMap.Scale','AMap.OverView'],
    function(){
        map.addControl(new AMap.ToolBar());
        map.addControl(new AMap.Scale());
    }
);

$('#city-select').editableSelect();
$('#area-select').editableSelect();

display_by_selected_area();

$('#search_btn').click(display_by_selected_area);

function display_by_selected_area(){
    var selected_area_name = $('#area-select').val();
    $.ajax({
        url: "/getAreaRevenueData",
        data: {"area_name": selected_area_name},
        method: 'GET',
        success: function(area_info) {
            var rlist = area_info['rlist'];
            var arlist = area_info['arlist'];
            var alist = area_info['alist'];
            create_restaurant_view(rlist, arlist, alist);
        }
    });
}

var rid2alistDict = {}; // <餐馆id, <aoi_list>>
var aid2aDict = {}; // <aoi_id, aoi>
var prevPoiMarkerList = []; // 在地图上显示的商家列表(marker列表)
/**
 * 创建显示餐馆的界面
 * @param rlist 餐馆列表
 * @param arlist <餐馆, aoi> 列表
 * @param alist <aoi>列表
 */
function create_restaurant_view(rlist, arlist, alist) {
    // step 0. init
    updateRestaurant2AoiListDict(arlist, alist);
    prevPoiMarkerList = [];
    // step 1. 偏移地图并刷新
    map.setCenter([parseFloat(rlist[0]['lng']) / 1000000, parseFloat(rlist[0]['lat']) / 1000000]);
    map.clearMap();
    // step 2. 显示餐馆位置
    for (var i = 0; i < rlist.length; i++) {
        var r = rlist[i];
        var circleMarker = new AMap.CircleMarker({
            center:[parseFloat(r['lng']) / 1000000, parseFloat(r['lat']) / 1000000],
            radius:8,
            strokeColor:'white',
            strokeWeight:2,
            strokeOpacity:0.5,
            fillColor:get_restaurant_color(r, getMetricType()),
            fillOpacity:0.5,
            zIndex:10,
            bubble:true,
            cursor:'pointer',
            clickable: true
        });
        circleMarker.setMap(map);
        circleMarker.ctnt = '名称: ' + r['name'] + '<br/>客单价: ' + r['avg_order_price'].toFixed(2) + '元<br/>单量(历史1个月): ' + r['total_order_cnt'] + '<br/>完成率: ' + (r['finish_order_cnt'] / r['total_order_cnt']).toFixed(3) + '<br/>相对准时率: ' + (r['relative_on_time_order_cnt'] / r['finish_order_cnt']).toFixed(3) + '<br/>平均配送距离: ' + r['delivery_distance'] + '米<br/>平均配送时长: ' + r['delivery_total_interval'].toFixed(2) + 'min<br/>单均成本: ' + r['avg_cost'].toFixed(3) + '元<br/>单均毛利: ' + r['avg_profit'].toFixed(3) + '元';
        circleMarker.rid = r['id'];
        circleMarker.rinfo = r;
        circleMarker.on('click', restaurantClick);
        prevPoiMarkerList.push(circleMarker);
        //circleMarker.emit('click', {target: circleMarker});
    }
}

var prevAoiList = []; // 在地图上显示的之前的AOI
function restaurantClick(e) {
    // step 0. 清空之前的显示
    map.remove(prevAoiList);
    prevAoiList = [];
    // step 1. 展示<餐馆, aoi>相关信息
    var arlist4r = rid2alistDict[e.target.rid];
    for (var i = 0; i < arlist4r.length; i++) {
        var ar = arlist4r[i];
        var polygonPath = getPolygonPath(aid2aDict[ar['userAOI_id']]['polygon']);
        var polygon = new AMap.Polygon({
            path: polygonPath,
            strokeOpacity: 0.5,
            fillOpacity: 0.5,
            fillColor: get_aoi_color(ar, getMetricType()),
            cursor:'pointer',
            clickable: true
        });
        prevAoiList.push(polygon);
        polygon.setMap(map);
        polygon.ctnt = '名称: ' + aid2aDict[ar['userAOI_id']]['name'] + '<br/>通行属性: ' + aid2aDict[ar['userAOI_id']]['io_attribute'] + '<br/>客单价: ' + ar['avg_order_price'].toFixed(2) + '元<br/>单量(历史1个月): ' + ar['total_order_cnt'] + '<br/>完成率: ' + (ar['finish_order_cnt'] / ar['total_order_cnt']).toFixed(3) + '<br/>相对准时率: ' + (ar['relative_on_time_order_cnt'] / ar['finish_order_cnt']).toFixed(3) + '<br/>平均配送距离: ' + ar['delivery_distance'] + '米<br/>平均配送时长: ' + ar['delivery_total_interval'].toFixed(2) + 'min<br/>单均成本: ' + ar['avg_cost'].toFixed(3) + '元<br/>单均毛利: ' + ar['avg_profit'].toFixed(3) + '元';
        polygon.ar = ar;
        polygon.on('click', aoiClick);
    }
    // step 2. 信息框提示
    infoWindow.setContent(e.target.ctnt);
    infoWindow.open(map, e.target.getCenter());
}

function updateRestaurant2AoiListDict(arlist, alist) {
    // step 1. 初始化
    rid2alistDict = {};
    aid2aDict = {};
    // step 2. 填充数据rid2alistDict
    for (var i = 0; i < arlist.length; i++) {
        var ar = arlist[i];
        if (! (ar['restaurant_id']  in rid2alistDict)) {
            rid2alistDict[ar['restaurant_id']] = [];
        }
        rid2alistDict[ar['restaurant_id']].push(ar);
    }
    // step 3. 填充数据aid2aDict
    for (var i = 0; i < alist.length; i++) {
        var a = alist[i];
        if (! (a['id'] in aid2aDict)) {
            aid2aDict[a['id']] = a;
        }
    }
}

function getPolygonPath(polygon_str) {
    var polygonPath = [];
    var ps_arr = polygon_str.replace("POLYGON ((", "").replace("))", "").split(",");
    for (var i = 0; i < ps_arr.length; i++) {
        var ps = ps_arr[i];
        var ps_info = ps.split(" ");
        polygonPath.push(new AMap.LngLat(parseFloat(ps_info[0]), parseFloat(ps_info[1])));
    }
    return polygonPath;
}

function aoiClick(e) {
    infoWindow4AOI.setContent(e.target.ctnt);
    infoWindow4AOI.open(map, e.target.getPath()[0]);
}

function processMetricTypeChange(chooseType) {
    // step 1. 信息显示更新
    if (chooseType == 'profit') {
        $('#red-info').html('红色: 毛利&le;-0.5元');
        $('#yellow-info').html('黄色: -0.5元&lt;毛利&lt;0.5元');
        $('#green-info').html('绿色: 毛利&ge;0.5元');
    } else if (chooseType == 'deliveryTime') {
        $('#red-info').html('红色: 配送时长&ge;40分钟');
        $('#yellow-info').html('黄色: 30分钟&lt;配送时长&lt;40分钟');
        $('#green-info').html('绿色: 配送时长&le;30分钟');
    } else if (chooseType == 'onTimeRatio') {
        $('#red-info').html('红色: 准时率&le;90%');
        $('#yellow-info').html('黄色: 90%&lt;准时率&lt;98%');
        $('#green-info').html('绿色: 准时率&ge;98%');
    } else if (chooseType == 'finishRatio') {
        $('#red-info').html('红色: 完成率&le;95%');
        $('#yellow-info').html('黄色: 95%&lt;完成率&lt;99%');
        $('#green-info').html('绿色: 完成率&ge;99%');
    }
    // step 2. 改变餐馆颜色
    for (var i = 0; i < prevPoiMarkerList.length; i++) {
        var marker = prevPoiMarkerList[i];
        var color = get_restaurant_color(marker.rinfo, chooseType);
        marker.setOptions({fillColor: color});
    }
    // step 3. 改变aoi颜色
    for (var i = 0; i < prevAoiList.length; i++) {
        var aoiPolygon = prevAoiList[i];
        var color = get_aoi_color(aoiPolygon.ar, chooseType);
        aoiPolygon.setOptions({fillColor: color});
    }
}

function getMetricType() {
    return $("input[type=radio][name='metricType']:checked").val();
}

$(document).ready(function () {
   $('input[type=radio][name=metricType]').change(function(){
        var chooseType = this.value;
        processMetricTypeChange(chooseType);
   });
});