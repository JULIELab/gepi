define(["jquery", "t5/core/zone"], function($, zoneManager) {

    function Widget(configuration) {
        this.handleId = configuration.handleId;
        this.widgetId = configuration.widgetId;
        this.toggleViewModeUrl = configuration.toggleViewModeUrl;
        this.refreshContentsUrl = configuration.refreshContentsUrl;
        this.zoneElementId = configuration.zoneElementId;
        this.widget = $("#" + this.widgetId);
        this.disableDefaultAjaxRefresh = configuration.disableDefaultAjaxRefresh;
        this.setupViewModeHandle();
    }
    Widget.prototype.getViewMode = function() {
        return this.widget.hasClass("large") ? "large" : "overview";
    };
    Widget.prototype.setupViewModeHandle = function() {
        let widget = this;
        // This sets the view mode class to the widget DIV, causing it to be larger or smaller
        $("#" + this.handleId).click(function() {
            let currentMode = widget.getViewMode();
            let newMode;
            switch (currentMode) {
                case "large":
                    $("body").removeClass("noScroll");
                    $("#widgetOverlay").removeClass("into");
                    newMode = "overview";
                    break;
                case "overview":
                    $("body").addClass("noScroll");
                    newMode = "large";
                    $("#widgetOverlay").addClass("into");
                    break;
            }
            //let left = document.getElementById(widget.widgetId).getBoundingClientRect().left;

            widget.widget.addClass(newMode).removeClass(currentMode);
            if (!widget.disableDefaultAjaxRefresh) {
                zoneManager.deferredZoneUpdate(widget.zoneElementId, widget.toggleViewModeUrl);
            }
        });
    };

    Widget.prototype.ajaxRefresh = function() {
        console.log("Issueing update of zone with ID " + this.zoneElementId);
        zoneManager.deferredZoneUpdate(this.zoneElementId, this.refreshContentsUrl);
    };



    let widgets = new Map();

    // This is called from GepiWidgetLayout#afterRender
    let addWidget = function(name, configuration) {
        widget = new Widget(configuration);
        widgets.set(name, widget);

        return widget;
    };

    let getWidget = function(name) {
        return widgets.get(name);
    };

    let refreshWidget = function(name) {
        getWidget(name).ajaxRefresh();
    };


    return {
        addWidget,
        getWidget,
        refreshWidget
    };
});