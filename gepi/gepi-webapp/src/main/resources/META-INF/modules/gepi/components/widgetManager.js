define(['jquery', 't5/core/zone'], function($, zoneManager) {
  function Widget(widgetObject) {
    this.widgetObject = widgetObject;
    this.handleId = widgetObject.handleId;
    this.widgetId = widgetObject.widgetId;
    this.toggleViewModeUrl = widgetObject.toggleViewModeUrl;
    this.refreshContentsUrl = widgetObject.refreshContentsUrl;
    this.zoneElementId = widgetObject.zoneElementId;
    this.widget = $('#' + this.widgetId);
    this.useTapestryZoneUpdates = widgetObject.useTapestryZoneUpdates;
    this.setupViewModeHandle();
  }
  Widget.prototype.getViewMode = function() {
    return this.widget.hasClass('large') ? 'large' : 'overview';
  };
  Widget.prototype.setupViewModeHandle = function() {
    const widget = this;
    // This sets the view mode class to the widget DIV,
    // causing it to be larger or smaller
    $('#' + this.handleId).click(function() {
      const currentMode = widget.getViewMode();
      let newMode;
      switch (currentMode) {
        case 'large':
          $('body').removeClass('noScroll');
          $('#widgetOverlay').removeClass('into');
          newMode = 'overview';
          break;
        case 'overview':
          $('body').addClass('noScroll');
          newMode = 'large';
          $('#widgetOverlay').addClass('into');
          break;
      }

      widget.widget.addClass(newMode).removeClass(currentMode);
      if (widget.useTapestryZoneUpdates) {
        zoneManager.deferredZoneUpdate(widget.zoneElementId, widget.toggleViewModeUrl);
      } else {
        widget.widgetObject.redraw();
      }
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
