define(['jquery', 't5/core/zone'], function($, zoneManager) {
  function Widget(widgetObject) {
    this.widgetObject = widgetObject;
    let widgetSettings = widgetObject.widgetSettings;
    this.handleId = widgetSettings.handleId;
    this.widgetId = widgetSettings.widgetId;
    this.toggleViewModeUrl = widgetSettings.toggleViewModeUrl;
    this.refreshContentsUrl = widgetSettings.refreshContentsUrl;
    this.zoneElementId = widgetSettings.zoneElementId;
    this.widget = $('#' + this.widgetId);
    this.handle = $('#' + this.handleId);
    this.chartArea = $('#' + this.widgetId.split('-')[0])
    this.useTapestryZoneUpdates = widgetSettings.useTapestryZoneUpdates;
    console.log("Creating Widget with settings " + JSON.stringify(widgetSettings))
    this.setupViewModeHandle();
  }
  Widget.prototype.getViewMode = function() {
    return this.widget.hasClass('large') ? 'large' : 'small';
  };
  Widget.prototype.setupViewModeHandle = function() {
    const widget = this;
    // This sets the view mode class to the widget DIV,
    // causing it to be larger or smaller
    $('#' + this.handleId).click(function() {
      const currentMode = widget.getViewMode();
      let newMode;
      let oldHandleClass;
      let newHandleClass;
      switch (currentMode) {
        case 'large':
          $('body').removeClass('noScroll');
          $('#disableplane').removeClass('show');
          newMode = 'small';
          oldHandleClass = 'widget-resize-to-small';
          newHandleClass = 'widget-resize-to-full';
          break;
        case 'small':
          $('body').addClass('noScroll');
          newMode = 'large';
          oldHandleClass = 'widget-resize-to-full';
          newHandleClass = 'widget-resize-to-small';
          window.setTimeout(() => $('#disableplane').addClass('show'), 500);
          break;
      }

      widget.widget.addClass(newMode).removeClass(currentMode);
      widget.handle.addClass(newHandleClass).removeClass(oldHandleClass);
      // jQuery selector with the widget object as context, i.e. as selector root:
      // this finds the element with the card-body class which is an descendant
      // of the widget.widget object
      $('.card-body', widget.widget).addClass(newMode).removeClass(currentMode);
      console.log("["+widget.widgetId+"] Setting widgetSetting viewMode to " + newMode)
      widget.widgetObject.widgetSettings.viewMode = newMode;
      if (widget.useTapestryZoneUpdates) {
        zoneManager.deferredZoneUpdate(widget.zoneElementId, widget.toggleViewModeUrl);
      } else {
        console.log("["+widget.widgetId+"] Redrawing widget after setting viewMode")
        widget.widgetObject.redraw();
      }
      console.log(widget.chartArea.parent())
    });
  };

  Widget.prototype.ajaxRefresh = function() {
    console.log('Issueing update of zone with ID ' + this.zoneElementId);
    zoneManager.deferredZoneUpdate(this.zoneElementId, this.refreshContentsUrl);
  };


  const widgets = new Map();

  // FOR ZONE UPDATES (Table widget)
  // This is called from GepiWidgetLayout#afterRender
  // for widgets to be updated via the Tapestry Zone update mechanism.
  const addWidget = function(name, widgetObject) {
    widgetWrapper = new Widget(widgetObject);
    widgets.set(name, widgetWrapper);

    return widgetWrapper;
  };

  const getWidget = function(name) {
    return widgets.get(name);
  };

  const refreshWidget = function(name) {
    console.log("Refresh for widget " + name + " requested.")
    getWidget(name).ajaxRefresh();
  };


  return {
    addWidget,
    getWidget,
    refreshWidget,
  };
});
