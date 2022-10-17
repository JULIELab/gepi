define(["bootstrap5/tooltip"], function(Tooltip) {
	var setuptooltips = function() {
		var tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'))
        var tooltipList = tooltipTriggerList.map(function (tooltipTriggerEl) {
          return new Tooltip(tooltipTriggerEl)
        })
	};
	return {
		"setuptooltips" : setuptooltips
	};
})