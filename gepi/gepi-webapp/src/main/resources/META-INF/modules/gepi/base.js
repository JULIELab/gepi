define(["jquery", "bootstrap/tooltip", "gridstack/gridstack.min",
"jquery-ui/core", "jquery-ui/widget", "jquery-ui/widgets/mouse", "jquery-ui/widgets/draggable", "jquery-ui/widgets/resizable"], function($) {
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