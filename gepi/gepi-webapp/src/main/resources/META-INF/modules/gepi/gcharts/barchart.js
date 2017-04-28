define([ "jquery", "gepi/pages/index" ], function($) {

    return function drawPieChart(barDat) {
        google.charts.setOnLoadCallback(function() {
            var data = new google.visualization.DataTable();
            data.addColumn('string', 'Gene');
            data.addColumn('number', 'Count');

            data.addRows(barDat)

            var options = {
            	chartArea : {
                    left : 10,
                    top : 10,
                    width : '100%',
                    height : '90%'
            	},
                legend : 'none',
                colors : [ '#76A7FA' ]
            };

            var chart = new google.visualization.BarChart(document.getElementById('barchart'));

            chart.draw(data, options);
        })
    }
})