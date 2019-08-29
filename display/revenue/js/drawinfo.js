var map = new AMap.Map('container',{
    resizeEnable: true,
    zoom: 14,
    center: poi_point
});

AMap.plugin(['AMap.ToolBar','AMap.Scale','AMap.OverView'],
    function(){
        map.addControl(new AMap.ToolBar());
        map.addControl(new AMap.Scale());
    }
);

var infoWindow = new AMap.InfoWindow({
    closeWhenClickMap: true
});

// 绘制商家的配送范围
var polygon_scope = new AMap.Polygon({
    map:map,
    path: poi_scope,
    strokeOpacity: 1.0,
    strokeColor:'black',
    strokeWeight: 5,
    fillOpacity: 0.2,
    fillColor: "#712C6B",
    cursor:'pointer',
    clickable: true
});

var polyEditor = new AMap.PolyEditor(map, polygon_scope);

polyEditor.on('addnode', function(event) {
    log.info('触发事件：addnode')
});

polyEditor.on('adjust', function(event) {
    log.info('触发事件：adjust')
});

polyEditor.on('removenode', function(event) {
    log.info('触发事件：removenode')
});

polyEditor.on('end', function(event) {
    log.info('触发事件： end');
    // event.target 即为编辑后的多边形对象
    // log.info(event.target);
    document.write(event.target);
});

// 显示在范围之内的AOI列表
for (var i = 0; i < scope_aoi.length; i++) {
    var polygon = new AMap.Polygon({
        map:map,
        path: scope_aoi[i],
        strokeOpacity: 0.5,
        fillOpacity: 0.8,
        fillColor: "#FFC300",
        cursor:'pointer',
        clickable: true
    });

    var info = [];
    info.push("<div><div style=\"padding:0px 0px 0px 4px;\"><b>" + scope_aoi_info[i]['name'] + "</b>");
    info.push("地址: " + scope_aoi_info[i]['addr']);
    info.push("面积大小: " + scope_aoi_info[i]['area']);
    info.push("AOI打分: " + scope_aoi_info[i]['score']);
    info.push("AOI单量: " + scope_aoi_info[i]['ordernum']);
    info.push("商家距离: " + scope_aoi_info[i]['dist'] + "</div></div>");

    polygon.ctnt = info.join("<br/>");
    polygon.on('click', polygonClick);
}

function polygonClick(e) {
    infoWindow.setContent(e.target.ctnt);
    infoWindow.open(map, e.lnglat);
}

// 显示不在范围内的AOI列表
for (var i = 0; i < not_in_scope_aois.length; i++) {
    var polygon = new AMap.Polygon({
        map: map,
        path: not_in_scope_aois[i],
        strokeOpacity: 0.5,
        fillOpacity: 0.8,
        fillColor: "#FF5733",
        cursor:'pointer',
        clickable: true
    });

    var info = [];
    info.push("<div><div style=\"padding:0px 0px 0px 4px;\"><b>" + not_in_scope_aoi_info[i]['name'] + "</b>");
    info.push("地址: " + not_in_scope_aoi_info[i]['addr']);
    info.push("面积大小: " + not_in_scope_aoi_info[i]['area']);
    info.push("AOI打分: " + not_in_scope_aoi_info[i]['score']);
    info.push("AOI单量: " + not_in_scope_aoi_info[i]['ordernum']);
    info.push("商家距离: " + not_in_scope_aoi_info[i]['dist'] + "</div></div>");

    polygon.ctnt = info.join("<br/>");
    polygon.on('click', polygonClick);
}

// 绘制商家当前的配送范围
// var polygon_old_scope = new AMap.Polygon({
//     path: original_scope,
//     strokeOpacity: 1.0,
//     strokeColor:'black',
//     strokeWeight: 5,
//     fillOpacity: 0.2,
//     fillColor: "#712C6B",
//     cursor:'pointer',
//     clickable: true
// });
// polygon_old_scope.setMap(map);

var circleMarker = new AMap.CircleMarker({
    center:poi_point,
    radius:10,
    strokeColor:'#512E5F',
    strokeWeight:2,
    strokeOpacity:1.0,
    fillColor:"#512E5F",
    fillOpacity:1.0,
    zIndex:10,
    bubble:true,
    cursor:'pointer',
    clickable: true
});
circleMarker.setMap(map);
circleMarker.ctnt = '商家';
circleMarker.on('click', markerClick);

function markerClick(e) {
    infoWindow.setContent(e.target.ctnt);
    infoWindow.open(map, e.target.getCenter());
}