define([ "jquery" ], function($) {

	return function drawPieChart(gepiDat) {

		var data = new google.visualization.DataTable();
		data.addColumn( 'string', 'Gene' );
		data.addColumn( 'number', 'Count' );
		
		data.addRows( gepiDat )
		
		var options = {
			title : 'Gene occurrences'
		};

		var chart = new google.visualization.PieChart(document
				.getElementById('piechart'));

		chart.draw(data, options);
	}


})