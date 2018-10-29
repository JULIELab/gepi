define([ "jquery", "gepi/pages/index" ], function($) {

    return function drawPieChart(pieDat) {
        google.charts.setOnLoadCallback(function() {
         $("#inputcol").data("animationtimer").then(() => {

            var data = new google.visualization.DataTable();
            data.addColumn('string', 'Gene');
            data.addColumn('number', 'Count');

            data.addRows(pieDat)

            var options = {
                chartArea : {
                    left : 10,
                    top : 10,
                    width : '100%',
                    height : '90%'
                },
                legend : {
                    position : 'right',
                    alignment : 'center'
                }
            };

            var chart = new google.visualization.PieChart(document.getElementById('piechart'));

            chart.draw(data, options);
            });
        });
    };
})