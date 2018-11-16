define([ "jquery", "t5/core/zone" ], function($, zoneManager, widgetSize) {
    var loadWidgetContent = function(url, zoneElementId) {
        console.log("Issueing update of zone with ID " + zoneElementId)
        zoneManager.deferredZoneUpdate(zoneElementId, url);
    };

    var setupViewModeHandle = function(handleId, widgetId, url, zoneElementId) {
        $("#" + handleId).click(function() {
            var widget = $("#" + widgetId);
            var currentMode = widget.hasClass("large") ? "large" : "overview";
            var newMode;
            switch (currentMode) {
            case "large":
                $("body").removeClass("noScroll");
                $("#widgetOverlay").removeClass("in");
                newMode = "overview";
                break;
            case "overview":
                $("body").addClass("noScroll");
                newMode = "large";
                $("#widgetOverlay").addClass("in");
                break;
            }
            var left = document.getElementById(widgetId).getBoundingClientRect().left;
            widget.addClass(newMode).removeClass(currentMode);
            zoneManager.deferredZoneUpdate(zoneElementId, url);
        });
    }
    
    return {
        "loadWidgetContent" : loadWidgetContent,
        "setupViewModeHandle" : setupViewModeHandle
    };
})
