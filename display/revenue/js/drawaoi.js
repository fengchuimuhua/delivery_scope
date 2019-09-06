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

var circleMarker = new AMap.CircleMarker({
    map:map,
    center:poi_point,
    radius:6,
    strokeColor:'#555555',
    strokeWeight:2,
    strokeOpacity:1.0,
    fillColor:"#555555",
    fillOpacity:1.0,
    bubble:true,
    cursor:'pointer',
    clickable: true,
    zIndex: 100
});
circleMarker.setMap(map);
circleMarker.ctnt = '商家';
circleMarker.on('click', markerClick);

function markerClick(e) {
    infoWindow.setContent(e.target.ctnt);
    infoWindow.open(map, e.target.getCenter());
}

var polyline = new AMap.Polyline({
    path: poi_scope_aoi,
    isOutline: true,
    outlineColor: '#555555',
    borderWeight: 1.5,
    strokeColor: "#555555",
    strokeOpacity: 1,
    strokeWeight: 2,
    // 折线样式还支持 'dashed'
    strokeStyle: "solid",
    // strokeStyle是dashed时有效
    strokeDasharray: [10, 5],
    lineJoin: 'round',
    lineCap: 'round',
    zIndex: 50
});

polyline.setMap(map);
// 缩放地图到合适的视野级别
map.setFitView([ polyline ]);

var polyEditor = new AMap.PolyEditor(map, polyline);
polyEditor.on('addnode', function(event) {
    log.info('触发事件：addnode')
})

polyEditor.on('adjust', function(event) {
    log.info('触发事件：adjust')
})

polyEditor.on('removenode', function(event) {
    log.info('触发事件：removenode')
})

polyEditor.on('end', function(event) {
    log.info('触发事件： end')
    // event.target 即为编辑后的折线对象
    document.write(event.target.getPath());
})

// 显示在范围之内的AOI列表
for (var i = 0; i < scope_aois_amap.length; i++) {
    var color;
    if (scope_aois_info[i]['ordernum'] > 6000) {
        color = "#23879E";
    } else if (scope_aois_info[i]['ordernum'] > 5000) {
        color = "#3F96AA";
    } else if (scope_aois_info[i]['ordernum'] > 4000) {
        color = "#5AA5B6";
    } else if (scope_aois_info[i]['ordernum'] > 3000) {
        color = "#76B4C2";
    } else if (scope_aois_info[i]['ordernum'] > 2000) {
        color = "#91C3CF";
    } else if (scope_aois_info[i]['ordernum'] > 1000) {
        color = "#ADD2DB";
    } else if (scope_aois_info[i]['ordernum'] > 500) {
        color = "#C8E1E7";
    } else {
        color = "#E4F0F3";
    }

    var polygon = new AMap.Polygon({
        map:map,
        path: scope_aois_amap[i],
        strokeOpacity: 0.1,
        strokeColor: "#555555",
        fillOpacity: 0.8,
        fillColor: color,
        cursor:'pointer',
        clickable: true
    });

    var info = [];
    info.push("<div><div style=\"padding:0px 0px 0px 4px;\"><b>" + scope_aois_info[i]['name'] + "</b>");
    info.push("地址: " + scope_aois_info[i]['addr']);
    info.push("面积大小: " + scope_aois_info[i]['area']);
    info.push("AOI打分: " + scope_aois_info[i]['score']);
    info.push("AOI单量: " + scope_aois_info[i]['ordernum']);
    info.push("商家距离: " + scope_aois_info[i]['dist'] + "</div></div>");

    polygon.ctnt = info.join("<br/>");
    polygon.on('click', polygonClick);
}

function polygonClick(e) {
    infoWindow.setContent(e.target.ctnt);
    infoWindow.open(map, e.lnglat);
}

// 显示不在范围内的BLOCK列表
for (var i = 0; i < not_in_scope_aois_amap.length; i++) {
    var color;
    if (not_in_scope_aois_info[i]['ordernum'] > 6000) {
        color = "#DACC22";
    } else if (not_in_scope_aois_info[i]['ordernum'] > 5000) {
        color = "#DFD23E";
    } else if (not_in_scope_aois_info[i]['ordernum'] > 4000) {
        color = "#E3D959";
    } else if (not_in_scope_aois_info[i]['ordernum'] > 3000) {
        color = "#E8DF75";
    } else if (not_in_scope_aois_info[i]['ordernum'] > 2000) {
        color = "#EDE691";
    } else if (not_in_scope_aois_info[i]['ordernum'] > 1000) {
        color = "#F1ECAC";
    } else if (not_in_scope_aois_info[i]['ordernum'] > 500) {
        color = "#F6F2C8";
    } else {
        color = "#FAF9E3";
    }

    var polygon = new AMap.Polygon({
        map: map,
        path: not_in_scope_aois_amap[i],
        strokeOpacity: 0.1,
        strokeColor: "#555555",
        fillOpacity: 0.8,
        fillColor: color,
        cursor:'pointer',
        clickable: true
    });

    var info = [];
    info.push("<div><div style=\"padding:0px 0px 0px 4px;\"><b>" + not_in_scope_aois_info[i]['name'] + "</b>");
    info.push("地址: " + not_in_scope_aois_info[i]['addr']);
    info.push("面积大小: " + not_in_scope_aois_info[i]['area']);
    info.push("AOI打分: " + not_in_scope_aois_info[i]['score']);
    info.push("AOI单量: " + not_in_scope_aois_info[i]['ordernum']);
    info.push("商家距离: " + not_in_scope_aois_info[i]['dist'] + "</div></div>");

    polygon.ctnt = info.join("<br/>");
    polygon.on('click', polygonClick);
}