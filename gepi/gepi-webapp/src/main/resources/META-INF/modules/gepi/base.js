// The gridstack names are configured as the "path" property in the requireJS configuration. This is
// done in Index.java#afterRender().
define(["jquery", "bootstrap/tooltip", "gridstack", "gridstack-jqueryui"], function($) {
	let setuptooltips = function() {
		$('[data-toggle="tooltip"]').tooltip();
	};
	let setupgridstack = function() {
		var options = {
			cellHeight: 80,
			verticalMargin: 10
		};
		$('.grid-stack').gridstack(options);
	};
	return {
		setuptooltips,
		setupgridstack
	};

})