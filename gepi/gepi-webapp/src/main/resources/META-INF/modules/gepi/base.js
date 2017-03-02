define([ "jquery", "bootstrap/tooltip" ], function($) {
	var setuptooltips = function() {
		$('[data-toggle="tooltip"]').tooltip();
	};
	return {
		"setuptooltips" : setuptooltips
	};

})