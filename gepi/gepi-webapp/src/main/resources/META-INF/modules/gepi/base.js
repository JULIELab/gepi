define(["jquery", "bootstrap5/tooltip"], function($, Tooltip) {
	var setuptooltips = function() {
		const makeTooltips = function() {
		var tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="default-tooltip"]'))
        var tooltipList = tooltipTriggerList.map(function (tooltipTriggerEl) {
          return new Tooltip(tooltipTriggerEl, {html:true})
        })
     };
     // re-enable tooltips after Ajax requests that might bring in new
     // elements into the DOM that should have tooltips
     $(document).ajaxComplete(function() {
     		makeTooltips();
     })
     makeTooltips();
	};
	return {
		"setuptooltips" : setuptooltips
	};
})