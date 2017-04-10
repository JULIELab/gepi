define(["jquery", "t5/core/zone"], function($, zoneManager) {
  var loadWidgetContent = function(url, zoneElementId) {
      zoneManager.deferredZoneUpdate(zoneElementId, url);
  };
  
  return {
    "loadWidgetContent": loadWidgetContent  
  };
})
