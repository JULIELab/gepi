define([ "jquery", "gepi/pages/index" ], function($) {

   // https://stackoverflow.com/a/39914235/1314955
   function sleep(ms) {
     return new Promise(resolve => setTimeout(resolve, ms));
   }

   async function draw(elementId, sankeyDat) {

       console.log("Drawing");
       var data = new google.visualization.DataTable();
       data.addColumn('string', 'FromGene');
       data.addColumn('string', 'ToGene');
       data.addColumn('number', 'Count');

       data.addRows(sankeyDat)

       var options = {
       };

       var chart = new google.visualization.Sankey(document.getElementById(elementId));

       chart.draw(data, options);
   }

    return function drawSankeyChart(elementId, sankeyDat) {
        google.charts.setOnLoadCallback(function() {
            $("#inputcol").data("animationtimer").then(() =>
                draw(elementId, sankeyDat));
        });
    };
})