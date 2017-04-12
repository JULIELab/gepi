define([ "jquery" ], function($) {

	return function drawPieChart( barDat ) {

		var data = new google.visualization.DataTable();
		data.addColumn( 'string', 'Gene' );
		data.addColumn( 'number', 'Count' );
		
		data.addRows( barDat )
		
		var options = {
			legend : 'none',
			colors : ['#76A7FA']
		};
		console.log(options)

		var chart = new google.visualization.BarChart(document
				.getElementById('barchart'));

		chart.draw(data, options);
	}
})