define(["bootstrap5/tooltip"], function(Tooltip) {
	var setuptooltips = function() {
		var tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="default-tooltip"]'))
        var tooltipList = tooltipTriggerList.map(function (tooltipTriggerEl) {
          return new Tooltip(tooltipTriggerEl, {html:true})
        })
	};
	return {
		"setuptooltips" : setuptooltips
	};
})