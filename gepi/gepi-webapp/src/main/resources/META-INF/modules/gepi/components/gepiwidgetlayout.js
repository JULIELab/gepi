define([ "jquery", "t5/core/zone" ], function($, zoneManager, widgetSize) {
    var loadWidgetContent = function(url, zoneElementId) {
        zoneManager.deferredZoneUpdate(zoneElementId, url);
    };

    var setupViewModeHandle = function(handleId, widgetId, url, zoneElementId) {
        $("#" + handleId).click(function() {
            var widget = $("#" + widgetId);
            var currentMode = widget.hasClass("large") ? "large" : "overview";
            var newMode;
            switch (currentMode) {
            case "large":
//                widget.removeClass("fixed");
                $("body").removeClass("noScroll");
                $("#widgetOverlay").removeClass("in");
                newMode = "overview";
                break;
            case "overview":
//                widget.addClass("fixed");
                $("body").addClass("noScroll");
                newMode = "large";
                $("#widgetOverlay").addClass("in");
                break;
            }
            var left = document.getElementById(widgetId).getBoundingClientRect().left;
//            console.log(left)
//            var difference = left - 15;
//            console.log(difference)
//            widget.css("transform", "translate(-"+difference+"px)");
            widget.addClass(newMode).removeClass(currentMode);
            zoneManager.deferredZoneUpdate(zoneElementId, url);
        });
    }
    
    return {
        "loadWidgetContent" : loadWidgetContent,
        "setupViewModeHandle" : setupViewModeHandle
    };
})
