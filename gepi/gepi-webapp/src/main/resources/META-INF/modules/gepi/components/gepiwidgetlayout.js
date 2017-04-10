define([ "jquery", "t5/core/zone" ], function($, zoneManager, widgetSize) {
    var loadWidgetContent = function(url, zoneElementId) {
        zoneManager.deferredZoneUpdate(zoneElementId, url);
    };

    var setupViewModeHandle = function(handleId, widgetId, url, zoneElementId) {
        $("#" + handleId).click(function() {
            var currentMode = $("#" + widgetId).hasClass("large") ? "large" : "overview";
            var newMode;
            switch (currentMode) {
            case "large":
                $("#" + widgetId).removeClass("fixed");
                newMode = "overview";
                break;
            case "overview":
                $("#" + widgetId).addClass("fixed");
                newMode = "large";
                break;
            }
//            var left = document.getElementById(widgetId).getBoundingClientRect().left;
//            console.log(left)
//            var difference = left - 15;
//            console.log(difference)
//            $("#" + widgetId).css("transform", "translate(-"+difference+"px)");
            $("#" + widgetId).addClass(newMode).removeClass(currentMode);
            zoneManager.deferredZoneUpdate(zoneElementId, url);
        });
    }

    return {
        "loadWidgetContent" : loadWidgetContent,
        "setupViewModeHandle" : setupViewModeHandle
    };
})
