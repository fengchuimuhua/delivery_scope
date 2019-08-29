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

// 显示在范围之内的AOI列表
for (var i = 0; i < scope_aoi.length; i++) {
    var polygon = new AMap.Polygon({
        path: scope_aoi[i],
        strokeOpacity: 0.5,
        fillOpacity: 0.8,
        fillColor: "#FFC300",
        cursor:'pointer',
        clickable: true
    });

    // var polygon = new AMap.Polygon({
    //     map: map,
    //     fillOpacity:0.4,
    //     path: aoi_path
    // });
    polygon.setMap(map);
}

// 显示不在范围内的AOI列表
for (var i = 0; i < not_in_scope_aois.length; i++) {
    var polygon = new AMap.Polygon({
        path: not_in_scope_aois[i],
        strokeOpacity: 0.5,
        fillOpacity: 0.8,
        fillColor: "#FF5733",
        cursor:'pointer',
        clickable: true
    });
    polygon.setMap(map)
}

// 绘制商家的配送范围
var polygon_scope = new AMap.Polygon({
    path: poi_scope,
    strokeOpacity: 1.0,
    strokeColor:'black',
    strokeWeight: 5,
    fillOpacity: 0.2,
    fillColor: "#712C6B",
    cursor:'pointer',
    clickable: true
});
polygon_scope.setMap(map);

// 绘制商家当前的配送范围
var polygon_old_scope = new AMap.Polygon({
    path: original_scope,
    strokeOpacity: 1.0,
    strokeColor:'black',
    strokeWeight: 5,
    fillOpacity: 0.2,
    fillColor: "#712C6B",
    cursor:'pointer',
    clickable: true
    // strokeOpacity: 1.0,
    // strokeColor:'#273746',
    // strokeWeight: 5,
    // strokeStyle: 'dashed',
    // fillOpacity: 0.2,
    // fillColor: "#712C6B",
    // cursor:'pointer',
    // clickable: true
});
// polygon_old_scope.setMap(map);

// var polyEditor = new AMap.PolyEditor(map, polygon_scope);
//
// polyEditor.on('addnode', function(event) {
//     log.info('触发事件：addnode')
// })
//
// polyEditor.on('adjust', function(event) {
//     log.info('触发事件：adjust')
// })
//
// polyEditor.on('removenode', function(event) {
//     log.info('触发事件：removenode')
// })
//
// polyEditor.on('end', function(event) {
//     log.info('触发事件： end')
//     // event.target 即为编辑后的多边形对象
// })

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
