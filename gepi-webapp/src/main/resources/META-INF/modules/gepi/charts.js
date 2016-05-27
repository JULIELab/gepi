define([ "jquery" ], function($) {

	var plotPie = function(data) {
		console.log(data);
		$('div#pie').html('<span>The data: ' + JSON.stringify(data) + '</span>')
	}
	
	return {
		"plotPie" : plotPie
	};
})