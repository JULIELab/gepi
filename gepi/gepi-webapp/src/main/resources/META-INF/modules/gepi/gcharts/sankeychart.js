define([ "jquery", "gepi/pages/index" ], function($) {

    return function drawSankeyChart(elementId, sankeyDat) {
        google.charts.setOnLoadCallback(function() {
            var data = new google.visualization.DataTable();
            data.addColumn('string', 'FromGene');
            data.addColumn('string', 'ToGene');
            data.addColumn('number', 'Count');

            data.addRows(sankeyDat)

            var options = {
            };

            var chart = new google.visualization.Sankey(document.getElementById(elementId));

            chart.draw(data, options);
        })
    };
})