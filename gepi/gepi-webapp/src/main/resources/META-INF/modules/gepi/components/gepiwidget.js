define(["jquery", "t5/core/zone"], function($, zoneManager) {
  var loadWidgetContent = function(url, zoneElementId) {
      console.log(url)
      console.log(zoneElementId)
      zoneManager.deferredZoneUpdate(zoneElementId, url);
  };
  
  return {
    "loadWidgetContent": loadWidgetContent  
  };
})
